package com.netflix.java.refactor.ast

import com.netflix.java.refactor.diff.JavaSourceDiff
import com.netflix.java.refactor.parse.SourceFile
import com.netflix.java.refactor.refactor.RefactorTransaction
import com.netflix.java.refactor.search.*
import java.io.Serializable
import java.util.regex.Pattern

interface Tree {
    val formatting: Formatting

    fun <R> accept(v: AstVisitor<R>): R
    fun format(): Tree = throw NotImplementedError()
    fun print() = PrintVisitor().visit(this).trimIndent()
}

interface Statement : Tree
interface Expression : Tree {
    val type: Type?
}

/**
 * The stylistic surroundings of a tree element
 */
sealed class Formatting {

    /**
     * Formatting should be inferred and reified from surrounding context
     */
    object Infer : Formatting()

    class Reified(val prefix: String) : Formatting() {
        companion object {
            val Empty = Reified("")
        }
    }

    object None : Formatting()
}

sealed class Tr : Serializable, Tree {

    data class Annotation(var annotationType: Tree, // FieldAccess or Ident
                          var args: List<Expression>,
                          override val type: Type?,
                          override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitAnnotation(this)
    }

    data class ArrayAccess(val indexed: Expression,
                           val index: Expression,
                           override val type: Type?,
                           override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitArrayAccess(this)
    }

    data class Assign(val variable: Expression,
                      val operator: Operator,
                      val assignment: Expression,
                      override val type: Type?,
                      override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitAssign(this)
        
        data class Operator(override val formatting: Formatting): Tr() {
            override fun <R> accept(v: AstVisitor<R>): R = v.default(null)
        }
    }

    data class AssignOp(val variable: Expression,
                        val operator: Operator,
                        val assignment: Expression,
                        override val type: Type?,
                        override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitAssignOp(this)

        enum class Operator {
            // Arithmetic
            Addition,
            Subtraction, Multiplication, Division, Modulo,

            // Bitwise
            BitAnd,
            BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift
        }
    }

    data class Binary(val operator: Operator,
                      val left: Expression,
                      val right: Expression,
                      override val type: Type?,
                      override val formatting: Formatting) : Expression, Tr() {

        enum class Operator {
            // Arithmetic
            Addition,
            Subtraction, Multiplication, Division, Modulo,

            // Relational
            LessThan,
            GreaterThan, LessThanOrEqual, GreaterThanOrEqual, Equal, NotEqual,

            // Bitwise
            BitAnd,
            BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift,

            // Boolean
            Or,
            And
        }

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBinary(this)
    }

    data class Block(val statements: List<Statement>,
                     override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBlock(this)
    }

    data class Break(val label: String?,
                     override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBreak(this)
    }

    data class Case(val pattern: Expression?, // null for the default case
                    val statements: List<Statement>,
                    override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCase(this)
    }

    data class Catch(val param: VariableDecl, // FIXME why is this not Parentheses when If is?
                     val body: Block,
                     override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCatch(this)
    }

    data class ClassDecl(
            val annotations: List<Annotation>,
            val modifiers: List<Modifier>,
            val name: String,
            val typeParams: List<TypeParameter>,
            val definitions: List<Tree>,
            val extends: Tree?,
            val implements: List<Tree>,
            val type: Type?,
            override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitClassDecl(this)

        fun fields(): List<VariableDecl> = definitions.filterIsInstance<VariableDecl>()
        fun methods(): List<MethodDecl> = definitions.filterIsInstance<MethodDecl>()

        enum class Modifier {
            Public, Protected, Private, Abstract, Static, Final
        }
    }

    data class CompilationUnit(val source: SourceFile,
                               val packageDecl: Package?,
                               val imports: List<Import>,
                               val classDecls: List<ClassDecl>,
                               override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCompilationUnit(this)

        fun hasType(clazz: Class<*>): Boolean = HasType(clazz.name).visit(this)
        fun hasType(clazz: String): Boolean = HasType(clazz).visit(this)

        fun hasImport(clazz: Class<*>): Boolean = HasImport(clazz.name).visit(this)
        fun hasImport(clazz: String): Boolean = HasImport(clazz).visit(this)

        /**
         * Find fields defined on this class, but do not include inherited fields up the type hierarchy
         */
        fun findFields(clazz: Class<*>): List<Field> = FindFields(clazz.name, false).visit(this)

        fun findFields(clazz: String): List<Field> = FindFields(clazz, false).visit(this)

        /**
         * Find fields defined both on this class and visible inherited fields up the type hierarchy
         */
        fun findFieldsIncludingInherited(clazz: Class<*>): List<Field> = FindFields(clazz.name, true).visit(this)

        fun findFieldsIncludingInherited(clazz: String): List<Field> = FindFields(clazz, true).visit(this)

        fun findMethodCalls(signature: String): List<Method> = FindMethods(signature).visit(this)

        fun refactor() = RefactorTransaction(this)

        fun diff(body: Tr.CompilationUnit.() -> Unit): String {
            val diff = JavaSourceDiff(this)
            this.body()
            return diff.gitStylePatch()
        }

        fun beginDiff() = JavaSourceDiff(this)
    }

    data class Continue(val label: String?,
                        override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitContinue(this)
    }

    data class DoWhileLoop(val condition: Parentheses,
                           val body: Statement,
                           override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitDoWhileLoop(this)
    }

    object Empty : Statement, Expression, Tr() {
        override val type: Type? = null
        override val formatting: Formatting = Formatting.None
        override fun <R> accept(v: AstVisitor<R>): R = v.visitEmpty(this)
    }

    data class FieldAccess(val fieldName: String,
                           val target: Expression,
                           override val type: Type?,
                           override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitFieldAccess(this)
    }

    data class ForEachLoop(val variable: VariableDecl,
                           val iterable: Expression,
                           val body: Statement,
                           override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitForEachLoop(this)
    }

    data class ForLoop(val control: Control,
                       val body: Statement,
                       override val formatting: Formatting) : Statement, Tr() {
        
        override fun <R> accept(v: AstVisitor<R>): R = v.visitForLoop(this)
        
        data class Control(val init: List<Statement>,
                           val condition: Expression?,
                           val update: List<Statement>,
                           override val formatting: Formatting): Tr() {
            
            override fun <R> accept(v: AstVisitor<R>): R = v.default(null)
        }
    }

    data class Ident(val name: String,
                     override val type: Type?,
                     override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitIdentifier(this)
    }

    data class If(val ifCondition: Parentheses,
                  val thenPart: Statement,
                  val elsePart: Statement?,
                  override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitIf(this)
    }

    data class Import(val qualid: FieldAccess,
                      val static: Boolean,
                      override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitImport(this)

        fun matches(clazz: String): Boolean = when (qualid.fieldName) {
            "*" -> qualid.target.print() == clazz.split('.').takeWhile { it[0].isLowerCase() }.joinToString(".")
            else -> qualid.print() == clazz
        }

        companion object {
            fun build(fullyQualifiedName: String, static: Boolean = false): Import {
                val parts = fullyQualifiedName.split('.')
                val expr = parts.foldIndexed(Empty as Expression to "") { i, acc, part ->
                    val (target, subpackage) = acc
                    if (target == Empty) {
                        Ident(part, Type.Package.build(part), Formatting.Reified.Empty) to part
                    } else {
                        val fullName = "$subpackage.$part"
                        val fmt = if (i == parts.size - 1) Formatting.Reified(" ") else Formatting.Reified.Empty
                        if (part[0].isUpperCase() || i == parts.size - 1) {
                            FieldAccess(part, target, Type.Class.build(fullName), fmt) to fullName
                        } else {
                            FieldAccess(part, target, Type.Package.build(fullName), fmt) to fullName
                        }
                    }
                }

                return Import(expr.first as FieldAccess, static, Formatting.Infer)
            }
        }
    }

    data class InstanceOf(val expr: Expression,
                          val clazz: Tree,
                          override val type: Type?,
                          override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitInstanceOf(this)
    }

    data class Label(val label: String,
                     val statement: Statement,
                     override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitLabel(this)
    }

    data class Lambda(val params: List<VariableDecl>,
                      val body: Tree,
                      override val type: Type?,
                      override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitLambda(this)
    }

    data class Literal(val typeTag: Type.Tag,
                       val value: Any,
                       override val type: Type?,
                       override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitLiteral(this)

        /**
         * Primitive values sometimes contain a prefix and suffix that hold the special characters,
         * e.g. the "" around String, the L at the end of a long, etc.
         */
        fun <T> transformValue(transform: (T) -> Any): String {
            val valueMatcher = "(.*)${Pattern.quote(value.toString())}(.*)".toRegex().find(this.print().replace("\\", ""))
            return when (valueMatcher) {
                is MatchResult -> {
                    val (prefix, suffix) = valueMatcher.groupValues.drop(1)
                    @Suppress("UNCHECKED_CAST")
                    return "$prefix${transform(value as T)}$suffix"
                }
                else -> {
                    throw IllegalStateException("Encountered a literal `$this` that could not be transformed")
                }
            }
        }
    }

    data class MethodDecl(val annotations: List<Annotation>,
                          val modifiers: List<Modifier>,
                          val name: String,
                          val returnTypeExpr: Expression?,
                          val params: List<VariableDecl>,
                          val thrown: List<Expression>,
                          val body: Block,
                          val defaultValue: Expression?,
                          override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitMethod(this)

        enum class Modifier {
            Public, Protected, Private, Abstract, Static, Final
        }
    }

    data class MethodInvocation(val methodSelect: Expression,
                                val args: List<Expression>,
                                val genericSignature: Type.Method?,

            // in the case of generic signature parts, this concretizes 
            // them relative to the call site
                                val resolvedSignature: Type.Method?,

                                val declaringType: Type.Class?,

                                override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitMethodInvocation(this)

        override val type = resolvedSignature?.returnType

        fun returnType(): Type? = resolvedSignature?.returnType

        fun methodName(): String = when (methodSelect) {
            is FieldAccess -> methodSelect.fieldName
            is Ident -> methodSelect.name
            else -> throw IllegalStateException("Unexpected method select type ${methodSelect}")
        }
    }

    data class NewArray(val typeExpr: Expression,
                        val dimensions: List<Expression>,
                        val elements: List<Expression>,
                        override val type: Type?,
                        override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitNewArray(this)
    }

    data class NewClass(val encl: Expression?,
                        val typeArgs: List<Expression>,
                        val identifier: Expression,
                        val args: List<Expression>,
                        val classBody: ClassDecl?, // non-null for anonymous classes
                        override val type: Type?,
                        override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitNewClass(this)
    }

    data class Package(val expr: Expression,
                       override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitPackage(this)
    }

    data class Parentheses(val expr: Expression,
                           override val type: Type?,
                           override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitParentheses(this)
    }

    data class Primitive(val typeTag: Type.Tag,
                         override val type: Type?,
                         override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitPrimitive(this)
    }

    data class Return(val expr: Expression?,
                      override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitReturn(this)
    }

    data class Switch(val selector: Parentheses,
                      val cases: List<Case>,
                      override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitSwitch(this)
    }

    data class Synchronized(val lock: Parentheses,
                            val body: Block,
                            override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitSynchronized(this)
    }

    data class Ternary(val condition: Expression,
                       val truePart: Expression,
                       val falsePart: Expression,
                       override val type: Type?,
                       override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTernary(this)
    }

    data class Throw(val expr: Expression,
                     override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitThrow(this)
    }

    data class Try(val resources: List<VariableDecl>,
                   val body: Block,
                   val catchers: List<Catch>,
                   val finally: Block?,
                   override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTry(this)
    }

    data class TypeParameter(val name: String,
                             val bounds: List<Expression>,
                             val annotations: List<Annotation>,
                             override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTypeParameter(this)
    }

    /**
     * Increment and decrement operations are valid statements, other operations are not
     */
    data class Unary(val operator: Operator,
                     val expr: Expression,
                     override val type: Type?,
                     override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitUnary(this)

        sealed class Operator: Tr() {
            override fun <R> accept(v: AstVisitor<R>): R = v.default(null)
            
            // Arithmetic
            data class PreIncrement(override val formatting: Formatting): Operator()
            data class PreDecrement(override val formatting: Formatting): Operator()
            data class PostIncrement(override val formatting: Formatting): Operator()
            data class PostDecrement(override val formatting: Formatting): Operator()
            data class Positive(override val formatting: Formatting): Operator()
            data class Negative(override val formatting: Formatting): Operator()

            // Bitwise
            data class Complement(override val formatting: Formatting): Operator()
            
            // Boolean
            data class Not(override val formatting: Formatting): Operator()
        }
    }

    data class VariableDecl(
            val annotations: List<Annotation>,
            val modifiers: List<Modifier>,
            val name: String,
            val nameExpr: Expression?,
            val varType: Expression?,
            val initializer: Expression?,
            val type: Type?,
            override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitVariable(this)

        enum class Modifier {
            Public, Protected, Private, Abstract, Static, Final, Transient, Volatile
        }
    }

    data class WhileLoop(val condition: Parentheses,
                         val body: Statement,
                         override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitWhileLoop(this)
    }
}   
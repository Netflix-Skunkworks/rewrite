package com.netflix.java.refactor.ast

import com.netflix.java.refactor.diff.JavaSourceDiff
import com.netflix.java.refactor.parse.SourceFile
import com.netflix.java.refactor.refactor.RefactorTransaction
import com.netflix.java.refactor.search.*
import java.io.Serializable
import java.lang.IllegalStateException
import java.util.regex.Pattern

interface Tree {
    val formatting: Formatting

    fun <R> accept(v: AstVisitor<R>): R = v.default(null)
    fun format(): Tree = throw NotImplementedError()
    fun print() = PrintVisitor().visit(this).trimIndent()
}

interface Statement : Tree

interface Expression : Tree {
    val type: Type?
}

/**
 * A tree representing a simple or fully qualified name
 */
interface NameTree : Tree

/**
 * A tree identifying a type (e.g. a simple or fully qualified class name, a primitive, array, or parameterized type)
 */
interface TypeTree: Tree

interface TypeDeclarationTree: Tree {
    val type: Type?
    val annotations: List<Tr.Annotation>
    val modifiers: List<Tr.TypeModifier>
    val name: Tr.Ident
    val implements: List<Tree>
    val body: Tr.Block<Tree>

    fun methods(): List<Tr.MethodDecl>
    fun fields(): List<Tr.VariableDecl>
}

/**
 * The stylistic surroundings of a tree element
 */
sealed class Formatting {

    /**
     * Formatting should be inferred and reified from surrounding context
     */
    object Infer : Formatting()

    data class Reified(val prefix: String, var suffix: String = "") : Formatting() {
        companion object {
            val Empty = Reified("")
        }
    }

    object None : Formatting()
}

sealed class Tr : Serializable, Tree {

    data class Annotation(var annotationType: NameTree,
                          var args: Arguments?,
                          override val type: Type?,
                          override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitAnnotation(this)

        data class Arguments(val args: List<Expression>, override val formatting: Formatting): Tr()
    }

    data class ArrayAccess(val indexed: Expression,
                           val dimension: Dimension,
                           override val type: Type?,
                           override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitArrayAccess(this)

        data class Dimension(val index: Expression, override val formatting: Formatting): Tr()
    }

    data class Assign(val variable: NameTree,
                      val assignment: Expression,
                      override val type: Type?,
                      override val formatting: Formatting) : Expression, Statement, Tr() {
        override fun <R> accept(v: AstVisitor<R>): R = v.visitAssign(this)
    }

    data class AssignOp(val variable: Expression,
                        val operator: Operator,
                        val assignment: Expression,
                        override val type: Type?,
                        override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitAssignOp(this)

        sealed class Operator: Tr() {
            // Arithmetic
            data class Addition(override val formatting: Formatting) : Operator()
            data class Subtraction(override val formatting: Formatting) : Operator()
            data class Multiplication(override val formatting: Formatting) : Operator()
            data class Division(override val formatting: Formatting) : Operator()
            data class Modulo(override val formatting: Formatting) : Operator()

            // Bitwise
            data class BitAnd(override val formatting: Formatting) : Operator()
            data class BitOr(override val formatting: Formatting) : Operator()
            data class BitXor(override val formatting: Formatting) : Operator()
            data class LeftShift(override val formatting: Formatting) : Operator()
            data class RightShift(override val formatting: Formatting) : Operator()
            data class UnsignedRightShift(override val formatting: Formatting) : Operator()
        }
    }

    data class Binary(val left: Expression,
                      val operator: Operator,
                      val right: Expression,
                      override val type: Type?,
                      override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBinary(this)

        sealed class Operator: Tr() {
            // Arithmetic
            data class Addition(override val formatting: Formatting) : Operator()
            data class Subtraction(override val formatting: Formatting) : Operator()
            data class Multiplication(override val formatting: Formatting) : Operator()
            data class Division(override val formatting: Formatting) : Operator()
            data class Modulo(override val formatting: Formatting) : Operator()

            // Relational
            data class LessThan(override val formatting: Formatting) : Operator()
            data class GreaterThan(override val formatting: Formatting) : Operator()
            data class LessThanOrEqual(override val formatting: Formatting) : Operator()
            data class GreaterThanOrEqual(override val formatting: Formatting) : Operator()
            data class Equal(override val formatting: Formatting) : Operator()
            data class NotEqual(override val formatting: Formatting) : Operator()

            // Bitwise
            data class BitAnd(override val formatting: Formatting) : Operator()
            data class BitOr(override val formatting: Formatting) : Operator()
            data class BitXor(override val formatting: Formatting) : Operator()
            data class LeftShift(override val formatting: Formatting) : Operator()
            data class RightShift(override val formatting: Formatting) : Operator()
            data class UnsignedRightShift(override val formatting: Formatting) : Operator()

            // Boolean
            data class Or(override val formatting: Formatting) : Operator()
            data class And(override val formatting: Formatting) : Operator()
        }
    }

    data class Block<out T: Tree>(val statements: List<T>,
                                  override val formatting: Formatting,
                                  val endOfBlockSuffix: String) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBlock(this)
    }

    data class Break(val label: Ident?,
                     override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBreak(this)
    }

    data class Case(val pattern: Expression?, // null for the default case
                    val statements: List<Statement>,
                    override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCase(this)
    }

    data class Catch(val param: Parentheses,
                     val body: Block<Statement>,
                     override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCatch(this)
    }

    data class ClassDecl(
            override val annotations: List<Annotation>,
            override val modifiers: List<TypeModifier>,
            override val name: Ident,
            val typeParams: TypeParameters?,
            val extends: Tree?,
            override val implements: List<Tree>,
            override val body: Block<Tree>,
            override val type: Type?,
            override val formatting: Formatting) : TypeDeclarationTree, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitClassDecl(this)

        override fun fields(): List<VariableDecl> = body.statements.filterIsInstance<VariableDecl>()
        override fun methods(): List<MethodDecl> = body.statements.filterIsInstance<MethodDecl>()
    }

    data class CompilationUnit(val source: SourceFile,
                               val packageDecl: Package?,
                               val imports: List<Import>,
                               val typeDecls: List<TypeDeclarationTree>,
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

    data class Continue(val label: Ident?,
                        override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitContinue(this)
    }

    data class DoWhileLoop(val body: Statement,
                           val condition: Parentheses,
                           override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitDoWhileLoop(this)
    }

    data class Empty(override val formatting: Formatting) : Statement, Expression, TypeTree, NameTree, Tr() {
        override val type: Type? = null
        override fun <R> accept(v: AstVisitor<R>): R = v.visitEmpty(this)
    }

    data class EnumValue(val name: Ident,
                         val initializer: Arguments?,
                         override val formatting: Formatting): Statement, Tr() {
        override fun <R> accept(v: AstVisitor<R>): R = v.visitEnumValue(this)

        data class Arguments(val args: List<Expression>, override val formatting: Formatting): Tr()
    }

    data class EnumClass(override val annotations: List<Annotation>,
                         override val modifiers: List<TypeModifier>,
                         override val name: Ident,
                         override val implements: List<Tree>,
                         override val body: Block<Tree>,
                         override val type: Type?,
                         override val formatting: Formatting) : TypeDeclarationTree, Tr() {

        /**
         * Values will always occur before any fields, constructors, or methods
         */
        fun values(): List<EnumValue> = body.statements.filterIsInstance<EnumValue>()

        fun members(): List<Tree> = body.statements.filter { it !is EnumValue }

        override fun methods(): List<MethodDecl> = body.statements.filterIsInstance<MethodDecl>()
        override fun fields(): List<VariableDecl> = body.statements.filterIsInstance<VariableDecl>()

        override fun <R> accept(v: AstVisitor<R>): R = v.visitEnumClass(this)
    }

    data class FieldAccess(val target: Expression,
                           val name: Ident,
                           override val type: Type?,
                           override val formatting: Formatting) : Expression, NameTree, TypeTree, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitFieldAccess(this)
    }

    data class ForEachLoop(val control: Control,
                           val body: Statement,
                           override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitForEachLoop(this)

        data class Control(val variable: VariableDecl,
                           val iterable: Expression,
                           override val formatting: Formatting): Tr()
    }

    data class ForLoop(val control: Control,
                       val body: Statement,
                       override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitForLoop(this)

        data class Control(val init: List<Statement>,
                           val condition: Expression,
                           val update: List<Statement>,
                           override val formatting: Formatting): Tr()
    }

    data class Ident(val name: String,
                     override val type: Type?,
                     override val formatting: Formatting) : Expression, NameTree, TypeTree, Tr() {

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

        fun matches(clazz: String): Boolean = when (qualid.name.name) {
            "*" -> qualid.target.print() == clazz.split('.').takeWhile { it[0].isLowerCase() }.joinToString(".")
            else -> qualid.print() == clazz
        }
    }

    data class InstanceOf(val expr: Expression,
                          val clazz: Tree,
                          override val type: Type?,
                          override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitInstanceOf(this)
    }

    data class Label(val label: Ident,
                     val statement: Statement,
                     override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitLabel(this)
    }

    data class Lambda(val params: List<VariableDecl>,
                      val arrow: Arrow,
                      val body: Tree,
                      override val type: Type?,
                      override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitLambda(this)

        data class Arrow(override val formatting: Formatting): Tr()
    }

    data class Literal(val typeTag: Type.Tag,
                       val value: Any?,
                       override val type: Type?,
                       override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitLiteral(this)

        /**
         * Primitive values sometimes contain a prefix and suffix that hold the special characters,
         * e.g. the "" around String, the L at the end of a long, etc.
         */
        fun <T> transformValue(transform: (T) -> Any): String {
            val valueMatcher = "(.*)${Pattern.quote(value.toString())}(.*)".toRegex().find(this.print().replace("\\", ""))
            @Suppress("UNREACHABLE_CODE")
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
                          val typeParameters: TypeParameters?,
                          val returnTypeExpr: TypeTree?, // null for constructors
                          val name: Ident,
                          val params: Parameters,
                          val throws: List<Expression>,
                          val body: Block<Statement>?,
                          val defaultValue: Expression?,
                          override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitMethod(this)

        sealed class Modifier: Tr() {
            data class Public(override val formatting: Formatting) : Modifier()
            data class Protected(override val formatting: Formatting) : Modifier()
            data class Private(override val formatting: Formatting) : Modifier()
            data class Abstract(override val formatting: Formatting) : Modifier()
            data class Static(override val formatting: Formatting) : Modifier()
            data class Final(override val formatting: Formatting) : Modifier()
        }

        data class Parameters(val params: List<Statement>, override val formatting: Formatting): Tr()
    }

    data class MethodInvocation(val select: Expression?,
                                val typeParameters: TypeParameters?,
                                val name: Ident,
                                val args: Arguments,
                                val genericSignature: Type.Method?,
                                // in the case of generic signature parts, this concretizes
                                // them relative to the call site
                                val resolvedSignature: Type.Method?,
                                val declaringType: Type.Class?,
                                override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitMethodInvocation(this)

        override val type = resolvedSignature?.returnType

        fun returnType(): Type? = resolvedSignature?.returnType

        fun methodName(): String = when (select) {
            is FieldAccess -> select.name.name
            is Ident -> select.name
            else -> throw IllegalStateException("Unexpected method select type ${select}")
        }

        data class Arguments(val args: List<Expression>, override val formatting: Formatting): Tr()
        data class TypeParameters(val params: List<NameTree>, override val formatting: Formatting): Tr()
    }

    data class NewArray(val typeExpr: TypeTree,
                        val dimensions: List<Dimension>,
                        val initializer: Initializer?,
                        override val type: Type?,
                        override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitNewArray(this)

        data class Dimension(val size: Expression, override val formatting: Formatting): Tr()
        data class Initializer(val elements: List<Expression>, override val formatting: Formatting): Tr()
    }

    data class NewClass(val clazz: TypeTree,
                        val args: Arguments,
                        val classBody: Block<Tree>?, // non-null for anonymous classes
                        override val type: Type?,
                        override val formatting: Formatting) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitNewClass(this)

        data class Arguments(val args: List<Expression>, override val formatting: Formatting): Tr()
    }

    data class Package(val expr: Expression,
                       override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitPackage(this)
    }

    data class ParameterizedType(val clazz: NameTree,
                                 val typeArguments: TypeArguments?,
                                 override val formatting: Formatting): TypeTree, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitParameterizedType(this)

        data class TypeArguments(val args: List<NameTree>,
                                 override val formatting: Formatting): Tr()
    }

    data class Parentheses(val expr: Tree,
                           override val type: Type?,
                           override val formatting: Formatting) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitParentheses(this)
    }

    data class Primitive(val typeTag: Type.Tag,
                         override val type: Type?,
                         override val formatting: Formatting) : Expression, TypeTree, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitPrimitive(this)
    }

    data class Return(val expr: Expression?,
                      override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitReturn(this)
    }

    data class Switch(val selector: Parentheses,
                      val cases: Block<Case>,
                      override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitSwitch(this)
    }

    data class Synchronized(val lock: Parentheses,
                            val body: Block<Statement>,
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

    data class Throw(val exception: Expression,
                     override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitThrow(this)
    }

    data class Try(val resources: Resources?,
                   val body: Block<Statement>,
                   val catches: List<Catch>,
                   val finally: Finally?,
                   override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTry(this)

        data class Resources(val decls: List<VariableDecl>, override val formatting: Formatting): Tr()
        data class Finally(val block: Block<Statement>, override val formatting: Formatting): Tr()
    }

    sealed class TypeModifier: Tr() {
        data class Public(override val formatting: Formatting): TypeModifier()
        data class Protected(override val formatting: Formatting): TypeModifier()
        data class Private(override val formatting: Formatting): TypeModifier()
        data class Abstract(override val formatting: Formatting): TypeModifier()
        data class Static(override val formatting: Formatting): TypeModifier()
        data class Final(override val formatting: Formatting): TypeModifier()
    }

    data class TypeParameter(val annotations: List<Annotation>,
                             val name: NameTree,
                             val bounds: List<Expression>,
                             override val formatting: Formatting) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTypeParameter(this)
    }

    data class TypeParameters(val params: List<TypeParameter>, override val formatting: Formatting): Tr() {
        override fun <R> accept(v: AstVisitor<R>): R = v.visitTypeParameters(this)
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
            val varType: TypeTree,
            val varArgs: Varargs?,
            val dimensionsBeforeName: List<Dimension>,
            val name: Ident,
            val dimensionsAfterName: List<Dimension>, // thanks for making it hard, Java
            val initializer: Expression?,
            val type: Type?,
            override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitVariable(this)

        sealed class Modifier: Tr() {
            data class Public(override val formatting: Formatting): Modifier()
            data class Protected(override val formatting: Formatting): Modifier()
            data class Private(override val formatting: Formatting): Modifier()
            data class Abstract(override val formatting: Formatting): Modifier()
            data class Static(override val formatting: Formatting): Modifier()
            data class Final(override val formatting: Formatting): Modifier()
            data class Transient(override val formatting: Formatting): Modifier()
            data class Volatile(override val formatting: Formatting): Modifier()
        }

        data class Varargs(override val formatting: Formatting): Tr()
        data class Dimension(val whitespace: Tr.Empty, override val formatting: Formatting): Tr()
    }

    data class WhileLoop(val condition: Parentheses,
                         val body: Statement,
                         override val formatting: Formatting) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitWhileLoop(this)
    }
}

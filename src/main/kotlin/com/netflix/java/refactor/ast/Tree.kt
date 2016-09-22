package com.netflix.java.refactor.ast

import com.netflix.java.refactor.diff.JavaSourceDiff
import com.netflix.java.refactor.parse.RawSourceCode
import com.netflix.java.refactor.refactor.RefactorTransaction
import com.netflix.java.refactor.search.*
import java.io.Serializable
import java.util.regex.Pattern

interface Tree {
    fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R
    val source: Source
}

interface Statement: Tree
interface Expression: Tree {
    val type: Type?
}

/**
 * The source code of a particular Tree element
 */
sealed class Source {
    @Deprecated("Move this method to Tree and use a PrintVisitor to print the tree containing this source")
    abstract fun text(cu: Tr.CompilationUnit): String
    
    abstract fun length(): Int
    
    /**
     * Being injected into existing source code but position has not yet been reified
     */
    class Insertion(private val text: String): Source() {
        override fun length(): Int = text.length
        override fun text(cu: Tr.CompilationUnit) = text
    }

    class Positioned(val pos: IntRange, private val text: String, val prefix: String, val suffix: String): Source() {
        override fun length(): Int = text.length
        override fun text(cu: Tr.CompilationUnit): String = text
    }
    
    class Persisted(val pos: IntRange, val prefix: String, val suffix: String): Source() {
        override fun length(): Int = pos.endInclusive-pos.start
        override fun text(cu: Tr.CompilationUnit): String = cu.rawSource.text.substring(pos)
    }

    object All: Source() {
        override fun length(): Int = Int.MAX_VALUE
        override fun text(cu: Tr.CompilationUnit): String = cu.rawSource.text
    }
    
    object None: Source() {
        override fun length(): Int = 0
        override fun text(cu: Tr.CompilationUnit): String = 
                throw UnsupportedOperationException("There is no source associated with this AST node")
    }
}

sealed class Tr : Serializable, Tree {

    data class ArrayAccess(val indexed: Expression,
                           val index: Expression,
                           override val type: Type?,
                           override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitArrayAccess(this, cursor)
    }

    data class Assign(val variable: Expression,
                      val assignment: Expression,
                      override val type: Type?,
                      override val source: Source): Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitAssign(this, cursor)
    }

    data class AssignOp(val operator: Operator,
                        val variable: Expression,
                        val assignment: Expression,
                        override val type: Type?,
                        override val source: Source): Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitAssignOp(this, cursor)

        enum class Operator {
            // Arithmetic
            Addition, Subtraction, Multiplication, Division, Modulo,

            // Bitwise
            BitAnd, BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift
        }
    }
    
    data class Binary(val operator: Operator,
                      val left: Expression,
                      val right: Expression,
                      override val type: Type?,
                      override val source: Source): Expression, Tr() {

        enum class Operator {
            // Arithmetic
            Addition, Subtraction, Multiplication, Division, Modulo,

            // Relational
            LessThan, GreaterThan, LessThanOrEqual, GreaterThanOrEqual, Equal, NotEqual,

            // Bitwise
            BitAnd, BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift,

            // Boolean
            Or, And
        }

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitBinary(this, cursor)
    }

    data class Block(val statements: List<Statement>,
                     override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitBlock(this, cursor)
    }
    
    data class Break(val label: String?,
                     override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitBreak(this, cursor)
    }
    
    data class Case(val pattern: Expression?, // null for the default case
                    val statements: List<Statement>,
                    override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitCase(this, cursor)
    }
    
    data class Catch(val param: VariableDecl, // FIXME why is this not Parentheses when If is?
                     val body: Block,
                     override val source: Source): Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitCatch(this, cursor)
    }

    data class ClassDecl(
            val name: String,
            val fields: List<VariableDecl>,
            val methods: List<MethodDecl>,
            val extends: Tree?,
            val implements: List<Tree>,
            val type: Type?,
            override val source: Source): Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitClassDecl(this, cursor)
    }
    
    data class CompilationUnit(val rawSource: RawSourceCode,
                               val packageDecl: Expression?,
                               val imports: List<Import>,
                               val classDecls: List<ClassDecl>): Tr() {

        override val source: Source = Source.All

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitCompilationUnit(this, cursor)

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
            val diff = JavaSourceDiff(this, source, rawSource.path)
            this.body()
            return diff.gitStylePatch()
        }

        fun beginDiff() = JavaSourceDiff(this, source, rawSource.path)
    }

    data class Continue(val label: String?,
                        override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitContinue(this, cursor)
    }
    
    data class DoWhileLoop(val condition: Parentheses,
                           val body: Statement,
                           override val source: Source) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitDoWhileLoop(this, cursor)
    }

    object Empty: Statement, Expression, Tr() {
        override val type: Type? = null
        override val source: Source = Source.None
        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitEmpty(this, cursor)
    }
    
    data class FieldAccess(val fieldName: String,
                           val target: Expression,
                           override val type: Type?,
                           override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitSelect(this, cursor)
    }
    
    data class ForEachLoop(val variable: VariableDecl,
                           val iterable: Expression,
                           val body: Statement,
                           override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitForEachLoop(this, cursor)
    }
    
    data class ForLoop(val init: List<Statement>,
                       val condition: Expression?,
                       val update: List<Statement>,
                       val body: Statement,
                       override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitForLoop(this, cursor)
    }
    
    data class Ident(val name: String,
                     override val type: Type?,
                     override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitIdentifier(this, cursor)
    }
    
    data class If(val ifCondition: Parentheses,
                  val thenPart: Statement,
                  val elsePart: Statement?,
                  override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitIf(this, cursor)
    }

    data class Import(val qualid: FieldAccess,
                      val static: Boolean,
                      override val source: Source): Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitImport(this, cursor)

        fun matches(clazz: String, cu: CompilationUnit): Boolean = when(qualid.fieldName) {
            "*" -> qualid.target.source.text(cu) == clazz.split('.').takeWhile { it[0].isLowerCase() }.joinToString(".")
            else -> qualid.source.text(cu) == clazz
        }
        
        companion object {
            fun build(fullyQualifiedName: String, static: Boolean = false): Import {
                val parts = fullyQualifiedName.split('.')
                val expr = parts.foldIndexed(Empty as Expression to "") { i, acc, part ->
                    val (target, subpackage) = acc
                    if(target == Empty) {
                        Ident(part, Type.Package.build(part), Source.Insertion(part)) to part
                    }
                    else {
                        val fullName = "$subpackage.$part"
                        if(part[0].isUpperCase() || i == parts.size - 1) {
                            FieldAccess(part, target, Type.Class.build(fullName), Source.Insertion(fullName)) to fullName
                        }
                        else {
                            FieldAccess(part, target, Type.Package.build(fullName), Source.Insertion(fullName)) to fullName   
                        }
                    }
                }

                val importSource = Source.Insertion(if(static) "import static $fullyQualifiedName;" else "import $fullyQualifiedName;")
                return Import(expr.first as FieldAccess, static, importSource)
            }
        }
    }
    
    data class InstanceOf(val expr: Expression,
                          val clazz: Tree,
                          override val type: Type?,
                          override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitInstanceOf(this, cursor)
    }
    
    data class Label(val label: String,
                     val statement: Statement,
                     override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitLabel(this, cursor)
    }
    
    data class Lambda(val params: List<VariableDecl>,
                      val body: Tree,
                      override val type: Type?,
                      override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitLambda(this, cursor)
    }

    data class Literal(val typeTag: Type.Tag,
                       val value: Any,
                       override val type: Type?,
                       override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitLiteral(this, cursor)

        /**
         * Primitive values sometimes contain a prefix and suffix that hold the special characters,
         * e.g. the "" around String, the L at the end of a long, etc.
         */
        fun <T> transformValue(cu: CompilationUnit, transform: (T) -> Any): String {
            val valueMatcher = "(.*)${Pattern.quote(value.toString())}(.*)".toRegex().find(source.text(cu).replace("\\", ""))
            return when(valueMatcher) {
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
    
    data class MethodDecl(val name: String,
                          val returnTypeExpr: Expression?,
                          val params: List<VariableDecl>,
                          val thrown: List<Expression>,
                          val body: Block,
                          val defaultValue: Expression?,
                          override val source: Source): Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitMethod(this, cursor)
    }
    
    data class MethodInvocation(val methodSelect: Expression,
                                val args: List<Expression>,
                                val genericSignature: Type.Method?,

            // in the case of generic signature parts, this concretizes 
            // them relative to the call site
                                val resolvedSignature: Type.Method?,

                                val declaringType: Type.Class?,

                                override val source: Source): Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitMethodInvocation(this, cursor)

        override val type = resolvedSignature?.returnType

        fun returnType(): Type? = resolvedSignature?.returnType

        fun methodName(): String = when(methodSelect) {
            is FieldAccess -> methodSelect.fieldName
            is Ident -> methodSelect.name
            else -> throw IllegalStateException("Unexpected method select type ${methodSelect}")
        }
    }
    
    data class NewArray(val typeExpr: Expression,
                        val dimensions: List<Expression>,
                        val elements: List<Expression>,
                        override val type: Type?,
                        override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitNewArray(this, cursor)
    }

    data class NewClass(val encl: Expression?,
                        val typeargs: List<Expression>,
                        val identifier: Expression,
                        val args: List<Expression>,
                        val classBody: ClassDecl?, // non-null for anonymous classes
                        override val type: Type?,
                        override val source: Source): Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitNewClass(this, cursor)
    }
    
    data class Parentheses(val expr: Expression,
                           override val type: Type?,
                           override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitParentheses(this, cursor)
    }
    
    data class Primitive(val typeTag: Type.Tag,
                         override val type: Type?,
                         override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitPrimitive(this, cursor)
    }
    
    data class Return(val expr: Expression?,
                      override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitReturn(this, cursor)
    }

    data class Switch(val selector: Parentheses,
                      val cases: List<Case>,
                      override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitSwitch(this, cursor)
    }
    
    data class Synchronized(val lock: Parentheses,
                            val body: Block,
                            override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitSynchronized(this, cursor)
    }

    data class Ternary(val condition: Expression,
                       val truePart: Expression,
                       val falsePart: Expression,
                       override val type: Type?,
                       override val source: Source): Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitTernary(this, cursor)
    }

    data class Throw(val expr: Expression,
                     override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitThrow(this, cursor)
    }
    
    data class Try(val resources: List<VariableDecl>,
                   val body: Block,
                   val catchers: List<Catch>,
                   val finally: Block?,
                   override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitTry(this, cursor)
    }
    
    /**
     * Increment and decrement operations are valid statements, other operations are not
     */
    data class Unary(val operator: Operator,
                     val expr: Expression,
                     override val type: Type?,
                     override val source: Source): Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitUnary(this, cursor)

        enum class Operator {

            // Arithmetic
            PreIncrement, PreDecrement, PostIncrement, PostDecrement, Positive, Negative,

            // Bitwise
            Complement,
            // Boolean
            Not

        }
    }

    data class VariableDecl(
            val name: String,
            val nameExpr: Expression?,
            val varType: Expression?,
            val initializer: Expression?,
            val type: Type?,
            override val source: Source): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitVariable(this, cursor)
    }
    
    data class WhileLoop(val condition: Parentheses,
                         val body: Statement,
                         override val source: Source) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>, cursor: Cursor): R = v.visitWhileLoop(this, cursor)
    }
}
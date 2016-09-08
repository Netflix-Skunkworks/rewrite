package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.ast.Tree
import com.sun.source.tree.*
import com.sun.source.util.TreeScanner
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.nio.JavacPathFileManager
import com.sun.tools.javac.tree.EndPosTable
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import javax.lang.model.type.TypeKind
import javax.tools.JavaFileManager
import javax.tools.StandardLocation
import kotlin.properties.Delegates

class OracleJdkParser(classpath: List<Path>? = null): Parser(classpath) {
    val context = Context()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with contest
    private val compilerLog = object: Log(context) {}
    private val pfm = JavacPathFileManager(context, true, Charset.defaultCharset())

    private val compiler = JavaCompiler(context)

    private val logger = LoggerFactory.getLogger(OracleJdkParser::class.java)

    init {
        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position 
        // for every tree element
        compiler.genEndPos = true
        compilerLog.setWriters(PrintWriter(object: Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                logger.info(String(cbuf.slice(off..(off + len)).toCharArray()))
            }
            override fun flush() {}
            override fun close() {}
        }))
    }

    override fun parse(sourceFiles: List<Path>, sourceFactory: (Path) -> Source): List<CompilationUnit> {
        if(filteredClasspath != null) { // override classpath
            assert(context.get(JavaFileManager::class.java) === pfm)
            pfm.setLocation(StandardLocation.CLASS_PATH, filteredClasspath)
        }

        val fileObjects = pfm.getJavaFileObjects(*filterSourceFiles(sourceFiles).toTypedArray())

        val cus = fileObjects.map { Paths.get(it.toUri()) to compiler.parse(it) }.toMap()

        try {
            cus.values.enterAll()
            compiler.attribute(compiler.todo)
        } catch(ignore: Throwable) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
        }

        return cus.map {
            val (path, cu) = it
            toIntermediateAst(cu, path, sourceFactory)
        }
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private fun Collection<JCTree.JCCompilationUnit>.enterAll() {
        val enter = Enter.instance(context)
        val compilationUnits = com.sun.tools.javac.util.List.from(this.toTypedArray())
        enter.main(compilationUnits)
    }

    private fun toIntermediateAst(cu: JCTree.JCCompilationUnit, path: Path, sourceFactory: (Path) -> Source): CompilationUnit =
            object : TreeScanner<Tree, Unit>() {
                var endPosTable: EndPosTable by Delegates.notNull()
                var source: String by Delegates.notNull()

                override fun reduce(r1: Tree?, r2: Tree?) = r1 ?: r2

                @Suppress("UNCHECKED_CAST") private fun <T : Tree> com.sun.source.tree.Tree.convert(): T = scan(this, null) as T
                @Suppress("UNCHECKED_CAST") private fun <T : Tree> com.sun.source.tree.Tree.convertOrNull(): T? =
                        if (this is com.sun.source.tree.Tree) scan(this, null) as T? else null

                private fun <T : Tree> List<com.sun.source.tree.Tree>?.convert(): List<T> =
                        if (this == null) emptyList()
                        else map { it.convertOrNull<T>() }.filterNotNull()

                override fun visitCompilationUnit(node: CompilationUnitTree, p: Unit?): Tree {
                    endPosTable = (node as JCTree.JCCompilationUnit).endPositions
                    return CompilationUnit(
                            sourceFactory(path),
                            node.packageName.convertOrNull(),
                            node.imports.convert(),
                            node.typeDecls.filterIsInstance<JCTree.JCClassDecl>().convert(),
                            node.posRange()
                    )
                }

                override fun visitNewClass(node: NewClassTree, p: Unit?): Tree =
                        NewClass(
                                node.enclosingExpression.convertOrNull(),
                                node.typeArguments.convert(),
                                node.identifier.convert(),
                                node.arguments.convert(),
                                node.classBody.convertOrNull(),
                                (node as JCTree.JCNewClass).type.type(),
                                node.posRange()
                        )

//                var r = scan(node.modifiers, p)
//                r = scanAndReduce(node.typeParameters, p, r)
                override fun visitClass(node: ClassTree, p: Unit?): Tree =
                        ClassDecl(
                                node.simpleName.toString(),
                                node.members.filterIsInstance<JCTree.JCVariableDecl>().convert(),
                                node.members.filterIsInstance<JCTree.JCMethodDecl>()
                                        // we don't care about the compiler-inserted default constructor, 
                                        // since it will never be subject to refactoring
                                        .filter { it.modifiers.flags and Flags.GENERATEDCONSTR == 0L }
                                        .convert(),
                                node.extendsClause.convertOrNull(),
                                node.implementsClause.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitMethodInvocation(node: MethodInvocationTree, p: Unit?): Tree {
                    val meth = node as JCTree.JCMethodInvocation
                    val select = meth.methodSelect

                    val methSymbol = when (select) {
                        null -> null
                        is JCTree.JCIdent -> select.sym
                        is JCTree.JCFieldAccess -> select.sym
                        else -> throw IllegalArgumentException("Unexpected method select type $this")
                    }

                    return MethodInvocation(
                            scan(meth.meth, null) as Expression,
                            meth.args.map { scan(it, null) as Expression },
                            methSymbol.type().asMethod(),
                            select?.type.type().asMethod(),
                            methSymbol?.owner?.type().asClass(),
                            meth.posRange()
                    )
                }

                override fun visitMethod(node: MethodTree, p: Unit?): Tree =
                        MethodDecl(
                                node.name.toString(),
                                node.returnType.convertOrNull(), // only null when compilation problem (literally no return type)
                                node.parameters.convert(),
                                node.throws.convert(),
                                node.body.convert(),
                                node.defaultValue.convertOrNull(),
                                node.posRange()
                        )

                override fun visitBlock(node: BlockTree, p: Unit?): Tree =
                        Block(node.statements.convert(), node.posRange())

                override fun visitLiteral(node: LiteralTree, p: Unit?): Tree =
                        Literal(
                                (node as JCTree.JCLiteral).typetag.tag(),
                                node.value,
                                node.type(),
                                node.posRange()
                        )

                override fun visitPrimitiveType(node: PrimitiveTypeTree, p: Unit?): Tree =
                        Primitive(when (node.primitiveTypeKind) {
                            TypeKind.BOOLEAN -> Type.Tag.Boolean
                            TypeKind.BYTE -> Type.Tag.Byte
                            TypeKind.CHAR -> Type.Tag.Char
                            TypeKind.DOUBLE -> Type.Tag.Double
                            TypeKind.FLOAT -> Type.Tag.Float
                            TypeKind.INT -> Type.Tag.Int
                            TypeKind.LONG -> Type.Tag.Long
                            TypeKind.SHORT -> Type.Tag.Short
                            TypeKind.VOID -> Type.Tag.Void
                            else -> throw IllegalArgumentException("Unknown primitive type $this")
                        }, node.type(), node.posRange())

                override fun visitVariable(node: VariableTree, p: Unit?): Tree? = when (node.name.toString()) {
                    "<error>" -> null
                    else ->
                        VariableDecl(
                                node.name.toString(),
                                node.nameExpression.convertOrNull(),
                                (node as JCTree.JCVariableDecl).vartype.convertOrNull(),
                                node.init.convertOrNull(),
                                node.type(),
                                node.posRange()
                        )
                }

                override fun visitImport(node: ImportTree, p: Unit?): Tree =
                        Import(
                                node.qualifiedIdentifier.convert(),
                                node.isStatic,
                                node.posRange()
                        )

                override fun visitMemberSelect(node: MemberSelectTree, p: Unit?): Tree =
                        FieldAccess(
                                (node as JCTree.JCFieldAccess).name.toString(),
                                node.selected.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitIdentifier(node: IdentifierTree, p: Unit?): Tree =
                        Ident(
                                node.name.toString(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitBinary(node: BinaryTree, p: Unit?): Tree =
                        Binary(
                                when((node as JCTree.JCBinary).tag) {
                                    JCTree.Tag.PLUS -> Binary.Operator.Addition
                                    JCTree.Tag.MINUS -> Binary.Operator.Subtraction
                                    JCTree.Tag.DIV -> Binary.Operator.Division
                                    JCTree.Tag.MUL -> Binary.Operator.Multiplication
                                    JCTree.Tag.MOD -> Binary.Operator.Modulo
                                    JCTree.Tag.AND -> Binary.Operator.And
                                    JCTree.Tag.OR -> Binary.Operator.Or
                                    JCTree.Tag.BITAND -> Binary.Operator.BitAnd
                                    JCTree.Tag.BITOR -> Binary.Operator.BitOr
                                    JCTree.Tag.BITXOR -> Binary.Operator.BitXor
                                    JCTree.Tag.SL -> Binary.Operator.LeftShift
                                    JCTree.Tag.SR -> Binary.Operator.RightShift
                                    JCTree.Tag.USR -> Binary.Operator.UnsignedRightShift
                                    JCTree.Tag.LT -> Binary.Operator.LessThan
                                    JCTree.Tag.GT -> Binary.Operator.GreaterThan
                                    JCTree.Tag.LE -> Binary.Operator.LessThanOrEqual
                                    JCTree.Tag.GE -> Binary.Operator.GreaterThanOrEqual
                                    JCTree.Tag.EQ -> Binary.Operator.Equal
                                    JCTree.Tag.NE -> Binary.Operator.NotEqual
                                    else -> throw IllegalArgumentException("Unexpected binary tag ${node.tag}")
                                },
                                node.leftOperand.convert(),
                                node.rightOperand.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitUnary(node: UnaryTree, p: Unit?): Tree =
                        Unary(
                                when((node as JCTree.JCUnary).tag) {
                                    JCTree.Tag.POS -> Unary.Operator.Positive
                                    JCTree.Tag.NEG -> Unary.Operator.Negative
                                    JCTree.Tag.PREDEC -> Unary.Operator.PreDecrement
                                    JCTree.Tag.PREINC -> Unary.Operator.PreIncrement
                                    JCTree.Tag.POSTDEC -> Unary.Operator.PostDecrement
                                    JCTree.Tag.POSTINC -> Unary.Operator.PostIncrement
                                    JCTree.Tag.COMPL -> Unary.Operator.Complement
                                    JCTree.Tag.NOT -> Unary.Operator.Not
                                    else -> throw IllegalArgumentException("Unexpected unary tag ${node.tag}")
                                },
                                node.arg.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitForLoop(node: ForLoopTree, p: Unit?): Tree =
                        ForLoop(
                                node.initializer.convert(),
                                node.condition.convertOrNull(),
                                node.update.convert(),
                                node.statement.convert(),
                                node.posRange()
                        )

                override fun visitEnhancedForLoop(node: EnhancedForLoopTree, p: Unit?): Tree =
                        ForEachLoop(
                                node.variable.convert(),
                                node.expression.convert(),
                                node.statement.convert(),
                                node.posRange()
                        )

                override fun visitIf(node: IfTree, p: Unit?): Tree =
                        If(
                                node.condition.convert(),
                                node.thenStatement.convert(),
                                node.elseStatement.convertOrNull(),
                                node.posRange()
                        )

                override fun visitConditionalExpression(node: ConditionalExpressionTree, p: Unit?): Tree =
                        Ternary(
                                node.condition.convert(),
                                node.trueExpression.convert(),
                                node.falseExpression.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitWhileLoop(node: WhileLoopTree, p: Unit?): Tree =
                        WhileLoop(
                                node.condition.convert(),
                                node.statement.convert(),
                                node.posRange()
                        )

                override fun visitDoWhileLoop(node: DoWhileLoopTree, p: Unit?): Tree =
                        DoWhileLoop(
                                node.condition.convert(),
                                node.statement.convert(),
                                node.posRange()
                        )

                override fun visitBreak(node: BreakTree, p: Unit?): Tree =
                        Break(
                                node.label?.toString(),
                                node.posRange()
                        )

                override fun visitContinue(node: ContinueTree, p: Unit?): Tree =
                        Continue(
                                node.label?.toString(),
                                node.posRange()
                        )

                override fun visitLabeledStatement(node: LabeledStatementTree, p: Unit?): Tree =
                        Label(
                                node.label.toString(),
                                node.statement.convert(),
                                node.posRange()
                        )

                override fun visitReturn(node: ReturnTree, p: Unit?): Tree =
                        Return(
                                node.expression.convertOrNull(),
                                node.posRange()
                        )

                override fun visitSwitch(node: SwitchTree, p: Unit?): Tree =
                        Switch(
                                node.expression.convert(),
                                node.cases.convert(),
                                node.posRange()
                        )

                override fun visitCase(node: CaseTree, p: Unit?): Tree =
                        Case(
                                node.expression.convertOrNull(),
                                node.statements.convert(),
                                node.posRange()
                        )

                override fun visitAssignment(node: AssignmentTree, p: Unit?): Tree =
                        Assign(
                                node.variable.convert(),
                                node.expression.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitCompoundAssignment(node: CompoundAssignmentTree, p: Unit?): Tree =
                        AssignOp(
                                when((node as JCTree.JCAssignOp).tag) {
                                    JCTree.Tag.PLUS_ASG -> AssignOp.Operator.Addition
                                    JCTree.Tag.MINUS_ASG -> AssignOp.Operator.Subtraction
                                    JCTree.Tag.DIV_ASG -> AssignOp.Operator.Division
                                    JCTree.Tag.MUL_ASG -> AssignOp.Operator.Multiplication
                                    JCTree.Tag.MOD_ASG -> AssignOp.Operator.Modulo
                                    JCTree.Tag.BITAND_ASG -> AssignOp.Operator.BitAnd
                                    JCTree.Tag.BITOR_ASG -> AssignOp.Operator.BitOr
                                    JCTree.Tag.BITXOR_ASG -> AssignOp.Operator.BitXor
                                    JCTree.Tag.SL_ASG -> AssignOp.Operator.LeftShift
                                    JCTree.Tag.SR_ASG -> AssignOp.Operator.RightShift
                                    JCTree.Tag.USR_ASG -> AssignOp.Operator.UnsignedRightShift
                                    else -> throw IllegalArgumentException("Unexpected compound assignment tag ${node.tag}")
                                },
                                node.lhs.convert(),
                                node.rhs.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitThrow(node: ThrowTree, p: Unit?): Tree =
                        Throw(
                                node.expression.convert(),
                                node.posRange()
                        )

                override fun visitTry(node: TryTree, p: Unit?): Tree =
                        Try(
                                node.resources.convert(),
                                node.block.convert(),
                                node.catches.convert(),
                                node.finallyBlock.convertOrNull(),
                                node.posRange()
                        )

                override fun visitCatch(node: CatchTree, p: Unit?): Tree =
                        Catch(
                                node.parameter.convert(),
                                node.block.convert(),
                                node.posRange()
                        )

                override fun visitSynchronized(node: SynchronizedTree, p: Unit?): Tree =
                        Synchronized(
                                node.expression.convert(),
                                node.block.convert(),
                                node.posRange()
                        )

                override fun visitEmptyStatement(node: EmptyStatementTree, p: Unit?): Tree =
                        Empty(node.posRange())

                override fun visitParenthesized(node: ParenthesizedTree, p: Unit?): Tree =
                        Parentheses(
                                node.expression.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitInstanceOf(node: InstanceOfTree, p: Unit?): Tree =
                        InstanceOf(
                                node.expression.convert(),
                                node.type.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitNewArray(node: NewArrayTree, p: Unit?): Tree =
                        NewArray(
                                node.type.convert(),
                                node.dimensions.convert(),
                                node.initializers.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitArrayAccess(node: ArrayAccessTree, p: Unit?): Tree =
                        ArrayAccess(
                                node.expression.convert(),
                                node.index.convert(),
                                node.type(),
                                node.posRange()
                        )

                override fun visitLambdaExpression(node: LambdaExpressionTree, p: Unit?): Tree =
                        Lambda(
                                node.parameters.convert(),
                                node.body.convert(),
                                node.type(),
                                node.posRange()
                        )

                private val allFlagsMask = Type.Var.Flags.values().map { it.value }.reduce { f1, f2 -> f1 or f2 }
                
                private fun Symbol?.type(stack: List<Any?> = emptyList()): Type? {
                    val owner = { this?.owner?.type(stack.plus(this)) }
                    return when (this) {
                        is Symbol.ClassSymbol -> {
                            val fields = (this.members_field?.elements ?: emptyList())
                                    .filterIsInstance<Symbol.VarSymbol>()
                                    .map { 
                                        Type.Var(
                                                it.name.toString(), 
                                                it.type.type(stack.plus(this)).asClass(), 
                                                this.flags() or allFlagsMask
                                        )
                                    }
                            
                            Type.Class(this.className(), owner(), fields, null)
                        }
                        is Symbol.PackageSymbol -> Type.Package(this.fullname.toString(), owner())
                        is Symbol.MethodSymbol -> {
                            when (this.type) {
                                is com.sun.tools.javac.code.Type.ForAll -> 
                                    (this.type as com.sun.tools.javac.code.Type.ForAll).qtype.type(stack.plus(this))
                                else -> this.type.type(stack.plus(this))
                            }
                        }
                        is Symbol.VarSymbol -> Type.GenericTypeVariable(this.name.toString(), null)
                        else -> null
                    }
                }
                
                private fun com.sun.tools.javac.code.Type?.type(stack: List<Any?> = emptyList()): Type? {
                    if(stack.contains(this))
                        return Type.Class.Cyclic
                    
                    return when (this) {
                        is com.sun.tools.javac.code.Type.PackageType -> this.tsym.type(stack.plus(this))
                        is com.sun.tools.javac.code.Type.ClassType -> {
                            this.tsym.type(stack.plus(this)).asClass()?.copy(supertype = supertype_field.type(stack.plus(this)).asClass())
                        }
                        is com.sun.tools.javac.code.Type.MethodType -> {
                            // in the case of generic method parameters or return type, the types here are concretized relative to the call site
                            val returnType = this.restype?.type(stack.plus(this))
                            val args = this.argtypes.map { it.type(stack.plus(this)) }.filterNotNull()
                            Type.Method(returnType, args)
                        }
                        is com.sun.tools.javac.code.Type.TypeVar -> Type.GenericTypeVariable(this.tsym.name.toString(), this.bound.type(stack.plus(this)).asClass())
                        is com.sun.tools.javac.code.Type.JCPrimitiveType -> Type.Primitive(this.tag.tag())
                        is com.sun.tools.javac.code.Type.ArrayType -> Type.Array(this.elemtype.type(stack.plus(this))!!)
                        com.sun.tools.javac.code.Type.noType -> null
                        else -> null
                    }
                }

                private fun com.sun.source.tree.Tree.type(): Type? = (this as JCTree).type.type()
                
                private fun com.sun.source.tree.Tree.posRange(): IntRange = (this as JCTree).posRange()
                private fun JCTree.posRange(): IntRange =
                        if (getEndPosition(endPosTable) < 0)
                            0..0
                        else
                            startPosition..getEndPosition(endPosTable) - 1

                private fun TypeTag.tag() = when (this) {
                    TypeTag.BOOLEAN -> Type.Tag.Boolean
                    TypeTag.BYTE -> Type.Tag.Byte
                    TypeTag.CHAR -> Type.Tag.Char
                    TypeTag.DOUBLE -> Type.Tag.Double
                    TypeTag.FLOAT -> Type.Tag.Float
                    TypeTag.INT -> Type.Tag.Int
                    TypeTag.LONG -> Type.Tag.Long
                    TypeTag.SHORT -> Type.Tag.Short
                    TypeTag.VOID -> Type.Tag.Void
                    TypeTag.NONE -> Type.Tag.None
                    TypeTag.CLASS -> Type.Tag.Class
                    else -> throw IllegalArgumentException("Unknown type tag $this")
                }

            }.scan(cu, null) as CompilationUnit
}
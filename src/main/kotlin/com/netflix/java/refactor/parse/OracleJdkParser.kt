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

    override fun parse(sourceFiles: List<Path>, sourceFactory: (Path) -> RawSourceCode): List<Tr.CompilationUnit> {
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

    private fun toIntermediateAst(cu: JCTree.JCCompilationUnit, path: Path, sourceFactory: (Path) -> RawSourceCode): Tr.CompilationUnit =
            object : TreeScanner<Tree, Unit>() {
                var endPosTable: EndPosTable by Delegates.notNull()

                override fun reduce(r1: Tree?, r2: Tree?) = r1 ?: r2

                @Suppress("UNCHECKED_CAST") private fun <T : Tree> com.sun.source.tree.Tree.convert(): T = scan(this, null) as T
                @Suppress("UNCHECKED_CAST") private fun <T : Tree> com.sun.source.tree.Tree.convertOrNull(): T? =
                        if (this is com.sun.source.tree.Tree) scan(this, null) as T? else null

                private fun <T : Tree> List<com.sun.source.tree.Tree>?.convert(): List<T> =
                        if (this == null) emptyList()
                        else map { it.convertOrNull<T>() }.filterNotNull()

                override fun visitCompilationUnit(node: CompilationUnitTree, p: Unit?): Tree {
                    endPosTable = (node as JCTree.JCCompilationUnit).endPositions
                    return Tr.CompilationUnit(
                            sourceFactory(path),
                            node.packageName.convertOrNull(),
                            node.imports.convert(),
                            node.typeDecls.filterIsInstance<JCTree.JCClassDecl>().convert()
                    )
                }

                override fun visitNewClass(node: NewClassTree, p: Unit?): Tree =
                        Tr.NewClass(
                                node.enclosingExpression.convertOrNull(),
                                node.typeArguments.convert(),
                                node.identifier.convert(),
                                node.arguments.convert(),
                                node.classBody.convertOrNull(),
                                (node as JCTree.JCNewClass).type.type(),
                                node.source()
                        )

//                var r = scan(node.modifiers, p)
//                r = scanAndReduce(node.typeParameters, p, r)
                override fun visitClass(node: ClassTree, p: Unit?): Tree =
                        Tr.ClassDecl(
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
                                node.source()
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

                    return Tr.MethodInvocation(
                            scan(meth.meth, null) as Expression,
                            meth.args.map { scan(it, null) as Expression },
                            methSymbol.type().asMethod(),
                            select?.type.type().asMethod(),
                            methSymbol?.owner?.type().asClass(),
                            meth.source()
                    )
                }

                override fun visitMethod(node: MethodTree, p: Unit?): Tree =
                        Tr.MethodDecl(
                                node.name.toString(),
                                node.returnType.convertOrNull(), // only null when compilation problem (literally no return type)
                                node.parameters.convert(),
                                node.throws.convert(),
                                node.body.convert(),
                                node.defaultValue.convertOrNull(),
                                node.source()
                        )

                override fun visitBlock(node: BlockTree, p: Unit?): Tree =
                        Tr.Block(node.statements.convert(), node.source())

                override fun visitLiteral(node: LiteralTree, p: Unit?): Tree =
                        Tr.Literal(
                                (node as JCTree.JCLiteral).typetag.tag(),
                                node.value,
                                node.type(),
                                node.source()
                        )

                override fun visitPrimitiveType(node: PrimitiveTypeTree, p: Unit?): Tree =
                        Tr.Primitive(when (node.primitiveTypeKind) {
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
                        }, node.type(), node.source())

                override fun visitVariable(node: VariableTree, p: Unit?): Tree? = when (node.name.toString()) {
                    "<error>" -> null
                    else ->
                        Tr.VariableDecl(
                                node.name.toString(),
                                node.nameExpression.convertOrNull(),
                                (node as JCTree.JCVariableDecl).vartype.convertOrNull(),
                                node.init.convertOrNull(),
                                node.type(),
                                node.source()
                        )
                }

                override fun visitImport(node: ImportTree, p: Unit?): Tree =
                        Tr.Import(
                                node.qualifiedIdentifier.convert(),
                                node.isStatic,
                                node.source()
                        )

                override fun visitMemberSelect(node: MemberSelectTree, p: Unit?): Tree =
                        Tr.FieldAccess(
                                (node as JCTree.JCFieldAccess).name.toString(),
                                node.selected.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitIdentifier(node: IdentifierTree, p: Unit?): Tree =
                        Tr.Ident(
                            node.name.toString(),
                            node.type(),
                            node.source()
                        )

                override fun visitBinary(node: BinaryTree, p: Unit?): Tree =
                        Tr.Binary(
                                when((node as JCTree.JCBinary).tag) {
                                    JCTree.Tag.PLUS -> Tr.Binary.Operator.Addition
                                    JCTree.Tag.MINUS -> Tr.Binary.Operator.Subtraction
                                    JCTree.Tag.DIV -> Tr.Binary.Operator.Division
                                    JCTree.Tag.MUL -> Tr.Binary.Operator.Multiplication
                                    JCTree.Tag.MOD -> Tr.Binary.Operator.Modulo
                                    JCTree.Tag.AND -> Tr.Binary.Operator.And
                                    JCTree.Tag.OR -> Tr.Binary.Operator.Or
                                    JCTree.Tag.BITAND -> Tr.Binary.Operator.BitAnd
                                    JCTree.Tag.BITOR -> Tr.Binary.Operator.BitOr
                                    JCTree.Tag.BITXOR -> Tr.Binary.Operator.BitXor
                                    JCTree.Tag.SL -> Tr.Binary.Operator.LeftShift
                                    JCTree.Tag.SR -> Tr.Binary.Operator.RightShift
                                    JCTree.Tag.USR -> Tr.Binary.Operator.UnsignedRightShift
                                    JCTree.Tag.LT -> Tr.Binary.Operator.LessThan
                                    JCTree.Tag.GT -> Tr.Binary.Operator.GreaterThan
                                    JCTree.Tag.LE -> Tr.Binary.Operator.LessThanOrEqual
                                    JCTree.Tag.GE -> Tr.Binary.Operator.GreaterThanOrEqual
                                    JCTree.Tag.EQ -> Tr.Binary.Operator.Equal
                                    JCTree.Tag.NE -> Tr.Binary.Operator.NotEqual
                                    else -> throw IllegalArgumentException("Unexpected binary tag ${node.tag}")
                                },
                                node.leftOperand.convert(),
                                node.rightOperand.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitUnary(node: UnaryTree, p: Unit?): Tree =
                        Tr.Unary(
                                when((node as JCTree.JCUnary).tag) {
                                    JCTree.Tag.POS -> Tr.Unary.Operator.Positive
                                    JCTree.Tag.NEG -> Tr.Unary.Operator.Negative
                                    JCTree.Tag.PREDEC -> Tr.Unary.Operator.PreDecrement
                                    JCTree.Tag.PREINC -> Tr.Unary.Operator.PreIncrement
                                    JCTree.Tag.POSTDEC -> Tr.Unary.Operator.PostDecrement
                                    JCTree.Tag.POSTINC -> Tr.Unary.Operator.PostIncrement
                                    JCTree.Tag.COMPL -> Tr.Unary.Operator.Complement
                                    JCTree.Tag.NOT -> Tr.Unary.Operator.Not
                                    else -> throw IllegalArgumentException("Unexpected unary tag ${node.tag}")
                                },
                                node.arg.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitForLoop(node: ForLoopTree, p: Unit?): Tree =
                        Tr.ForLoop(
                                node.initializer.convert(),
                                node.condition.convertOrNull(),
                                node.update.convert(),
                                node.statement.convert(),
                                node.source()
                        )

                override fun visitEnhancedForLoop(node: EnhancedForLoopTree, p: Unit?): Tree =
                        Tr.ForEachLoop(
                                node.variable.convert(),
                                node.expression.convert(),
                                node.statement.convert(),
                                node.source()
                        )

                override fun visitIf(node: IfTree, p: Unit?): Tree =
                        Tr.If(
                                node.condition.convert(),
                                node.thenStatement.convert(),
                                node.elseStatement.convertOrNull(),
                                node.source()
                        )

                override fun visitConditionalExpression(node: ConditionalExpressionTree, p: Unit?): Tree =
                        Tr.Ternary(
                                node.condition.convert(),
                                node.trueExpression.convert(),
                                node.falseExpression.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitWhileLoop(node: WhileLoopTree, p: Unit?): Tree =
                        Tr.WhileLoop(
                                node.condition.convert(),
                                node.statement.convert(),
                                node.source()
                        )

                override fun visitDoWhileLoop(node: DoWhileLoopTree, p: Unit?): Tree =
                        Tr.DoWhileLoop(
                                node.condition.convert(),
                                node.statement.convert(),
                                node.source()
                        )

                override fun visitBreak(node: BreakTree, p: Unit?): Tree =
                        Tr.Break(
                                node.label?.toString(),
                                node.source()
                        )

                override fun visitContinue(node: ContinueTree, p: Unit?): Tree =
                        Tr.Continue(
                                node.label?.toString(),
                                node.source()
                        )

                override fun visitLabeledStatement(node: LabeledStatementTree, p: Unit?): Tree =
                        Tr.Label(
                                node.label.toString(),
                                node.statement.convert(),
                                node.source()
                        )

                override fun visitReturn(node: ReturnTree, p: Unit?): Tree =
                        Tr.Return(
                                node.expression.convertOrNull(),
                                node.source()
                        )

                override fun visitSwitch(node: SwitchTree, p: Unit?): Tree =
                        Tr.Switch(
                                node.expression.convert(),
                                node.cases.convert(),
                                node.source()
                        )

                override fun visitCase(node: CaseTree, p: Unit?): Tree =
                        Tr.Case(
                                node.expression.convertOrNull(),
                                node.statements.convert(),
                                node.source()
                        )

                override fun visitAssignment(node: AssignmentTree, p: Unit?): Tree =
                        Tr.Assign(
                                node.variable.convert(),
                                node.expression.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitCompoundAssignment(node: CompoundAssignmentTree, p: Unit?): Tree =
                        Tr.AssignOp(
                                when((node as JCTree.JCAssignOp).tag) {
                                    JCTree.Tag.PLUS_ASG -> Tr.AssignOp.Operator.Addition
                                    JCTree.Tag.MINUS_ASG -> Tr.AssignOp.Operator.Subtraction
                                    JCTree.Tag.DIV_ASG -> Tr.AssignOp.Operator.Division
                                    JCTree.Tag.MUL_ASG -> Tr.AssignOp.Operator.Multiplication
                                    JCTree.Tag.MOD_ASG -> Tr.AssignOp.Operator.Modulo
                                    JCTree.Tag.BITAND_ASG -> Tr.AssignOp.Operator.BitAnd
                                    JCTree.Tag.BITOR_ASG -> Tr.AssignOp.Operator.BitOr
                                    JCTree.Tag.BITXOR_ASG -> Tr.AssignOp.Operator.BitXor
                                    JCTree.Tag.SL_ASG -> Tr.AssignOp.Operator.LeftShift
                                    JCTree.Tag.SR_ASG -> Tr.AssignOp.Operator.RightShift
                                    JCTree.Tag.USR_ASG -> Tr.AssignOp.Operator.UnsignedRightShift
                                    else -> throw IllegalArgumentException("Unexpected compound assignment tag ${node.tag}")
                                },
                                node.lhs.convert(),
                                node.rhs.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitThrow(node: ThrowTree, p: Unit?): Tree =
                        Tr.Throw(
                                node.expression.convert(),
                                node.source()
                        )

                override fun visitTry(node: TryTree, p: Unit?): Tree =
                        Tr.Try(
                                node.resources.convert(),
                                node.block.convert(),
                                node.catches.convert(),
                                node.finallyBlock.convertOrNull(),
                                node.source()
                        )

                override fun visitCatch(node: CatchTree, p: Unit?): Tree =
                        Tr.Catch(
                                node.parameter.convert(),
                                node.block.convert(),
                                node.source()
                        )

                override fun visitSynchronized(node: SynchronizedTree, p: Unit?): Tree =
                        Tr.Synchronized(
                                node.expression.convert(),
                                node.block.convert(),
                                node.source()
                        )

                override fun visitEmptyStatement(node: EmptyStatementTree, p: Unit?): Tree = Tr.Empty

                override fun visitParenthesized(node: ParenthesizedTree, p: Unit?): Tree =
                        Tr.Parentheses(
                                node.expression.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitInstanceOf(node: InstanceOfTree, p: Unit?): Tree =
                        Tr.InstanceOf(
                                node.expression.convert(),
                                node.type.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitNewArray(node: NewArrayTree, p: Unit?): Tree =
                        Tr.NewArray(
                                node.type.convert(),
                                node.dimensions.convert(),
                                node.initializers.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitArrayAccess(node: ArrayAccessTree, p: Unit?): Tree =
                        Tr.ArrayAccess(
                                node.expression.convert(),
                                node.index.convert(),
                                node.type(),
                                node.source()
                        )

                override fun visitLambdaExpression(node: LambdaExpressionTree, p: Unit?): Tree =
                        Tr.Lambda(
                                node.parameters.convert(),
                                node.body.convert(),
                                node.type(),
                                node.source()
                        )

                private val allFlagsMask = Type.Var.Flags.values().map { it.value }.reduce { f1, f2 -> f1 or f2 }
                
                private fun Symbol?.type(stack: List<Any?> = emptyList()): Type? {
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
                            
                            Type.Class.build(this.className(), fields, null)
                        }
                        is Symbol.PackageSymbol -> Type.Package.build(this.fullname.toString())
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
                
                private fun com.sun.source.tree.Tree.source(): Source = (this as JCTree).source()
                
                private fun JCTree.source(): Source =
                        if (getEndPosition(endPosTable) < 0)
                            Source.None
                        else
                            Source.Persisted(startPosition..getEndPosition(endPosTable) - 1, "", "") // FIXME correctly calculate prefix and suffix

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

            }.scan(cu, null) as Tr.CompilationUnit
}
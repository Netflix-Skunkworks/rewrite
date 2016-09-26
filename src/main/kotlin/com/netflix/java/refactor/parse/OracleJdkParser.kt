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
import java.util.*
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind
import javax.tools.JavaFileManager
import javax.tools.StandardLocation
import kotlin.properties.Delegates

class OracleJdkParser(classpath: List<Path>? = null) : Parser(classpath) {
    val context = Context()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with contest
    private val compilerLog = object : Log(context) {}
    private val pfm = JavacPathFileManager(context, true, Charset.defaultCharset())

    private val compiler = JavaCompiler(context)

    private val logger = LoggerFactory.getLogger(OracleJdkParser::class.java)

    init {
        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true
        compilerLog.setWriters(PrintWriter(object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                logger.info(String(cbuf.slice(off..(off + len)).toCharArray()))
            }

            override fun flush() {
            }

            override fun close() {
            }
        }))
    }

    override fun parse(sourceFiles: List<Path>): List<Tr.CompilationUnit> {
        if (filteredClasspath != null) { // override classpath
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
            toIntermediateAst(cu, path, path.toFile().readText())
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

    private fun toIntermediateAst(cu: JCTree.JCCompilationUnit, path: Path, source: String): Tr.CompilationUnit {
        return object : TreeScanner<Tree, Formatting>() {
            var endPosTable: EndPosTable by Delegates.notNull()
            override fun reduce(r1: Tree?, r2: Tree?) = r1 ?: r2

            val nodeStack = Stack<JCTree>()

            override fun scan(node: com.sun.source.tree.Tree, p: Formatting): Tree {
                nodeStack.push(node as JCTree)
                val t = super.scan(node, p)
                nodeStack.pop()
                return t
            }

            @Suppress("UNCHECKED_CAST")
            private fun <T : Tree> com.sun.source.tree.Tree.convert(fmt: Formatting): T = scan(this, fmt) as T

            @Suppress("UNCHECKED_CAST")
            private fun <T : Tree> com.sun.source.tree.Tree.convert(predecessors: List<com.sun.source.tree.Tree?> = emptyList(), trimLeft: Regex? = null): T =
                    convert(this.formatLeft(predecessors.toList(), trimLeft))

            @Suppress("UNCHECKED_CAST")
            private fun <T : Tree> com.sun.source.tree.Tree.convertOrNull(predecessors: List<com.sun.source.tree.Tree?> = emptyList(), trimLeft: Regex? = null): T? =
                    if (this is com.sun.source.tree.Tree) scan(this, this.formatLeft(predecessors.toList(), trimLeft)) as T? else null

            @Suppress("UNCHECKED_CAST")
            private fun <T : Tree> com.sun.source.tree.Tree.convertOrNull(predecessors: List<com.sun.source.tree.Tree?> = emptyList(), fmt: Formatting): T? =
                if (this is com.sun.source.tree.Tree) scan(this, fmt) as T? else null

            private fun <T : Tree> List<com.sun.source.tree.Tree>?.convert(predecessors: List<com.sun.source.tree.Tree?> = emptyList(), trimLeft: Regex? = null): List<T> =
                    if (this == null) emptyList()
                    else {
                        mapIndexed { i, tree ->
                            tree.convert<T>(predecessors + if(i > 0) this.subList(0, i-1) else emptyList(),
                                    if(i == 0) trimLeft else null)
                        }
                    }

            private fun com.sun.source.tree.Tree.formatLeft(predecessors: List<com.sun.source.tree.Tree?> = emptyList(), trimLeft: Regex? = null): Formatting.Reified {
                val sibling = predecessors.findLast { it != null }
                val prefix = if(sibling != null) {
                    source.substring((sibling as JCTree).getEndPosition(endPosTable) + 1,
                            (this as JCTree).startPosition)
                } else {
                    source.substring(nodeStack.peek().startPosition, (this as JCTree).startPosition)
                }

                val trimmedPrefix = if(trimLeft != null) prefix.replaceFirst(trimLeft, "") else prefix

                return Formatting.Reified(trimmedPrefix)
            }

            override fun visitCompilationUnit(node: CompilationUnitTree, fmt: Formatting): Tree {
                endPosTable = (node as JCTree.JCCompilationUnit).endPositions

                val packageExpr: Expression? = node.packageName.convertOrNull(trimLeft = "^package".toRegex())

                return Tr.CompilationUnit(
                        SourceFile.fromText(path.toString(), source),
                        if(packageExpr is Expression) Tr.Package(packageExpr, node.packageName.formatLeft(trimLeft = "package\\s+\$".toRegex())) else null,
                        node.imports.convert(listOf(node.packageName)),
                        node.typeDecls.filterIsInstance<JCTree.JCClassDecl>().convert(listOf(node.packageName) + node.imports),
                        fmt
                )
            }

            override fun visitNewClass(node: NewClassTree, fmt: Formatting): Tree =
                    Tr.NewClass(
                            node.enclosingExpression.convertOrNull(),
                            node.typeArguments.convert(),
                            node.identifier.convert(),
                            node.arguments.convert(),
                            node.classBody.convertOrNull(),
                            (node as JCTree.JCNewClass).type.type(),
                            fmt
                    )

            override fun visitClass(node: ClassTree, fmt: Formatting): Tree =
                    Tr.ClassDecl(
                            node.modifiers.annotations.convert(),
                            node.modifiers.flags.map {
                                when(it) {
                                    Modifier.PUBLIC -> Tr.ClassDecl.Modifier.Public
                                    Modifier.PROTECTED -> Tr.ClassDecl.Modifier.Protected
                                    Modifier.PRIVATE -> Tr.ClassDecl.Modifier.Private
                                    Modifier.ABSTRACT -> Tr.ClassDecl.Modifier.Abstract
                                    Modifier.STATIC -> Tr.ClassDecl.Modifier.Static
                                    Modifier.FINAL -> Tr.ClassDecl.Modifier.Final
                                    else -> throw IllegalArgumentException("Unexpected modifier $it")
                                }
                            },
                            node.simpleName.toString(),
                            node.typeParameters.convert(),
                            // we don't care about the compiler-inserted default constructor,
                            // since it will never be subject to refactoring
                            node.members
                                    .filter { it !is JCTree.JCMethodDecl || it.modifiers.flags and Flags.GENERATEDCONSTR == 0L }
                                    .convert(),
                            node.extendsClause.convertOrNull(),
                            node.implementsClause.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitTypeParameter(node: TypeParameterTree, fmt: Formatting): Tree =
                    Tr.TypeParameter(
                            node.name.toString(),
                            node.bounds.convert(),
                            node.annotations.convert(),
                            fmt
                    )

            override fun visitAnnotation(node: AnnotationTree, fmt: Formatting): Tree {
                val args: List<Expression> = if(node.arguments.size == 1) {
                    val arg = node.arguments[0] as JCTree.JCAssign
                    listOf(if(arg.getEndPosition(endPosTable) < 0) {
                        // this is the "value" argument, but without an explicit "value = ..."
                        arg.rhs.convert(listOf(node.annotationType))
                    } else {
                        // this is either an explicit "value" argument or is assigning some other property
                        arg.convert(listOf(node.annotationType))
                    })
                } else node.arguments.convert(listOf(node.annotationType))

                return Tr.Annotation(
                        node.annotationType.convert(trimLeft = "^@".toRegex()),
                        args,
                        node.type(),
                        fmt
                )
            }

            override fun visitMethodInvocation(node: MethodInvocationTree, fmt: Formatting): Tree {
                val meth = node as JCTree.JCMethodInvocation
                val select = meth.methodSelect

                val methSymbol = when (select) {
                    null -> null
                    is JCTree.JCIdent -> select.sym
                    is JCTree.JCFieldAccess -> select.sym
                    else -> throw IllegalArgumentException("Unexpected method select type $this")
                }

                return Tr.MethodInvocation(
                        meth.meth.convert(),
                        meth.args.convert(),
                        methSymbol.type().asMethod(),
                        select?.type.type().asMethod(),
                        methSymbol?.owner?.type().asClass(),
                        fmt
                )
            }

            override fun visitMethod(node: MethodTree, fmt: Formatting): Tree =
                    Tr.MethodDecl(
                            node.modifiers.annotations.convert(),
                            node.modifiers.flags.map {
                                when(it) {
                                    Modifier.PUBLIC -> Tr.MethodDecl.Modifier.Public
                                    Modifier.PROTECTED -> Tr.MethodDecl.Modifier.Protected
                                    Modifier.PRIVATE -> Tr.MethodDecl.Modifier.Private
                                    Modifier.ABSTRACT -> Tr.MethodDecl.Modifier.Abstract
                                    Modifier.STATIC -> Tr.MethodDecl.Modifier.Static
                                    Modifier.FINAL -> Tr.MethodDecl.Modifier.Final
                                    else -> throw IllegalArgumentException("Unexpected modifier $it")
                                }
                            },
                            node.name.toString(),
                            node.returnType.convertOrNull(), // only null when compilation problem (literally no return type)
                            node.parameters.convert(),
                            node.throws.convert(),
                            node.body.convert(),
                            node.defaultValue.convertOrNull(),
                            fmt
                    )

            override fun visitBlock(node: BlockTree, fmt: Formatting): Tree =
                    Tr.Block(node.statements.convert(), fmt)

            override fun visitLiteral(node: LiteralTree, fmt: Formatting): Tree =
                    Tr.Literal(
                            (node as JCTree.JCLiteral).typetag.tag(),
                            node.value,
                            node.type(),
                            fmt
                    )

            override fun visitPrimitiveType(node: PrimitiveTypeTree, fmt: Formatting): Tree =
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
                    }, node.type(), fmt)

            override fun visitVariable(node: VariableTree, fmt: Formatting): Tree? {
                val opSource = source.substring((node as JCTree.JCVariableDecl).nameExpression.getEndPosition(endPosTable),
                        node.init.startPosition)

                return when (node.name.toString()) {
                    "<error>" -> null
                    else ->
                        Tr.VariableDecl(
                                node.modifiers.annotations.convert(),
                                node.modifiers.getFlags().map {
                                    when(it) {
                                        Modifier.PUBLIC -> Tr.VariableDecl.Modifier.Public
                                        Modifier.PROTECTED -> Tr.VariableDecl.Modifier.Protected
                                        Modifier.PRIVATE -> Tr.VariableDecl.Modifier.Private
                                        Modifier.ABSTRACT -> Tr.VariableDecl.Modifier.Abstract
                                        Modifier.STATIC -> Tr.VariableDecl.Modifier.Static
                                        Modifier.FINAL -> Tr.VariableDecl.Modifier.Final
                                        Modifier.TRANSIENT -> Tr.VariableDecl.Modifier.Transient
                                        Modifier.VOLATILE -> Tr.VariableDecl.Modifier.Volatile
                                        else -> throw IllegalArgumentException("Unexpected modifier $it")
                                    }
                                },
                                node.name.toString(),
                                node.nameExpression.convertOrNull(listOf(node.modifiers), "^((public)|(protected)|(private)|(abstract)|(static)|(final)|(transient)|(volatile))(\s+((public)|(protected)|(private)|(abstract)|(static)|(final)|(transient)|(volatile))*)".toRegex()),
                                node.vartype.convertOrNull(),
                                node.init.convertOrNull(listOf(node.modifiers, node.nameExpression, node.vartype), Formatting.Reified(opSource.substringAfter("="))),
                                node.type(),
                                fmt
                        )
                }
            }

            override fun visitImport(node: ImportTree, fmt: Formatting): Tree {
                return Tr.Import(
                        node.qualifiedIdentifier.convert(trimLeft = "^import(\\s+static)?".toRegex()),
                        node.isStatic,
                        fmt
                )
            }

            override fun visitMemberSelect(node: MemberSelectTree, fmt: Formatting): Tree =
                    Tr.FieldAccess(
                            (node as JCTree.JCFieldAccess).name.toString(),
                            node.selected.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitIdentifier(node: IdentifierTree, fmt: Formatting): Tree =
                    Tr.Ident(
                            node.name.toString(),
                            node.type(),
                            fmt
                    )

            override fun visitBinary(node: BinaryTree, fmt: Formatting): Tree =
                    Tr.Binary(
                            when ((node as JCTree.JCBinary).tag) {
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
                            fmt
                    )

            override fun visitUnary(node: UnaryTree, fmt: Formatting): Tree =
                Tr.Unary(
                        when ((node as JCTree.JCUnary).tag) {
                            JCTree.Tag.POS -> Tr.Unary.Operator.Positive(Formatting.Reified.Empty)
                            JCTree.Tag.NEG -> Tr.Unary.Operator.Negative(Formatting.Reified.Empty)
                            JCTree.Tag.PREDEC -> Tr.Unary.Operator.PreDecrement(Formatting.Reified.Empty)
                            JCTree.Tag.PREINC -> Tr.Unary.Operator.PreIncrement(Formatting.Reified.Empty)
                            JCTree.Tag.POSTDEC -> Tr.Unary.Operator.PostDecrement(node.formatLeft(listOf(node.arg)))
                            JCTree.Tag.POSTINC -> Tr.Unary.Operator.PostIncrement(node.formatLeft(listOf(node.arg)))
                            JCTree.Tag.COMPL -> Tr.Unary.Operator.Complement(Formatting.Reified.Empty)
                            JCTree.Tag.NOT -> Tr.Unary.Operator.Not(Formatting.Reified.Empty)
                            else -> throw IllegalArgumentException("Unexpected unary tag ${node.tag}")
                        },
                        node.arg.convert(trimLeft = when (node.tag) {
                            JCTree.Tag.POS -> "^+".toRegex()
                            JCTree.Tag.NEG -> "^-".toRegex()
                            JCTree.Tag.PREDEC -> "^--".toRegex()
                            JCTree.Tag.PREINC -> "^\\+\\+".toRegex()
                            JCTree.Tag.COMPL -> "^~".toRegex()
                            JCTree.Tag.NOT -> "^!".toRegex()
                            else -> null
                        }),
                        node.type(),
                        fmt
                )

            override fun visitForLoop(node: ForLoopTree, fmt: Formatting): Tree {
                val controlPrefix = source
                        .substring((node as JCTree.JCForLoop).startPosition..node.getEndPosition(endPosTable))
                        .substringAfter("for")
                        .substringBefore("(")

                return Tr.ForLoop(
                        Tr.ForLoop.Control(
                                node.initializer.convert(),
                                node.condition.convertOrNull(node.initializer, ";".toRegex()),
                                node.update.convert(node.initializer + node.condition, ";+".toRegex()),
                                Formatting.Reified(controlPrefix)
                        ),
                        node.statement.convert(),
                        fmt
                )
            }

            override fun visitEnhancedForLoop(node: EnhancedForLoopTree, fmt: Formatting): Tree =
                    Tr.ForEachLoop(
                            node.variable.convert(),
                            node.expression.convert(),
                            node.statement.convert(),
                            fmt
                    )

            override fun visitIf(node: IfTree, fmt: Formatting): Tree =
                    Tr.If(
                            node.condition.convert(),
                            node.thenStatement.convert(),
                            node.elseStatement.convertOrNull(),
                            fmt
                    )

            override fun visitConditionalExpression(node: ConditionalExpressionTree, fmt: Formatting): Tree =
                    Tr.Ternary(
                            node.condition.convert(),
                            node.trueExpression.convert(),
                            node.falseExpression.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitWhileLoop(node: WhileLoopTree, fmt: Formatting): Tree =
                    Tr.WhileLoop(
                            node.condition.convert(),
                            node.statement.convert(),
                            fmt
                    )

            override fun visitDoWhileLoop(node: DoWhileLoopTree, fmt: Formatting): Tree =
                    Tr.DoWhileLoop(
                            node.condition.convert(),
                            node.statement.convert(),
                            fmt
                    )

            override fun visitBreak(node: BreakTree, fmt: Formatting): Tree =
                    Tr.Break(
                            node.label?.toString(),
                            fmt
                    )

            override fun visitContinue(node: ContinueTree, fmt: Formatting): Tree =
                    Tr.Continue(
                            node.label?.toString(),
                            fmt
                    )

            override fun visitLabeledStatement(node: LabeledStatementTree, fmt: Formatting): Tree =
                    Tr.Label(
                            node.label.toString(),
                            node.statement.convert(),
                            fmt
                    )

            override fun visitReturn(node: ReturnTree, fmt: Formatting): Tree =
                    Tr.Return(
                            node.expression.convertOrNull(),
                            fmt
                    )

            override fun visitSwitch(node: SwitchTree, fmt: Formatting): Tree =
                    Tr.Switch(
                            node.expression.convert(),
                            node.cases.convert(),
                            fmt
                    )

            override fun visitCase(node: CaseTree, fmt: Formatting): Tree =
                    Tr.Case(
                            node.expression.convertOrNull(),
                            node.statements.convert(),
                            fmt
                    )

            override fun visitAssignment(node: AssignmentTree, fmt: Formatting): Tree {
                val opSource = source.substring((node as JCTree.JCAssign).variable.getEndPosition(endPosTable),
                        node.expression.startPosition)

                return Tr.Assign(
                        node.variable.convert(),
                        Tr.Assign.Operator(Formatting.Reified(opSource.substringBefore("="))),
                        node.expression.convert(Formatting.Reified(opSource.substringAfter("="))),
                        node.type(),
                        fmt
                )
            }

            override fun visitCompoundAssignment(node: CompoundAssignmentTree, fmt: Formatting): Tree =
                    Tr.AssignOp(
                            (node as JCTree.JCAssignOp).lhs.convert(),
                            when (node.tag) {
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
                            node.rhs.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitThrow(node: ThrowTree, fmt: Formatting): Tree =
                    Tr.Throw(
                            node.expression.convert(),
                            fmt
                    )

            override fun visitTry(node: TryTree, fmt: Formatting): Tree =
                    Tr.Try(
                            node.resources.convert(),
                            node.block.convert(),
                            node.catches.convert(),
                            node.finallyBlock.convertOrNull(),
                            fmt
                    )

            override fun visitCatch(node: CatchTree, fmt: Formatting): Tree =
                    Tr.Catch(
                            node.parameter.convert(),
                            node.block.convert(),
                            fmt
                    )

            override fun visitSynchronized(node: SynchronizedTree, fmt: Formatting): Tree =
                    Tr.Synchronized(
                            node.expression.convert(),
                            node.block.convert(),
                            fmt
                    )

            override fun visitEmptyStatement(node: EmptyStatementTree, fmt: Formatting): Tree = Tr.Empty

            override fun visitParenthesized(node: ParenthesizedTree, fmt: Formatting): Tree =
                    Tr.Parentheses(
                            node.expression.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitInstanceOf(node: InstanceOfTree, fmt: Formatting): Tree =
                    Tr.InstanceOf(
                            node.expression.convert(),
                            node.type.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitNewArray(node: NewArrayTree, fmt: Formatting): Tree =
                    Tr.NewArray(
                            node.type.convert(),
                            node.dimensions.convert(),
                            node.initializers.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitArrayAccess(node: ArrayAccessTree, fmt: Formatting): Tree =
                    Tr.ArrayAccess(
                            node.expression.convert(),
                            node.index.convert(),
                            node.type(),
                            fmt
                    )

            override fun visitLambdaExpression(node: LambdaExpressionTree, fmt: Formatting): Tree =
                    Tr.Lambda(
                            node.parameters.convert(),
                            node.body.convert(),
                            node.type(),
                            fmt
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
                if (stack.contains(this))
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
                TypeTag.CLASS -> Type.Tag.String
                else -> throw IllegalArgumentException("Unknown type tag $this")
            }

        }.scan(cu, Formatting.Reified.Empty) as Tr.CompilationUnit
    }
}
package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.ast.Tree
import com.sun.source.tree.*
import com.sun.source.util.TreeScanner
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.EndPosTable
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.code.BoundKind
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind
import kotlin.properties.Delegates

class OracleJdkParserVisitor(val path: Path, val source: String): TreeScanner<Tree, Formatting.Reified>() {
    private typealias JdkTree = com.sun.source.tree.Tree

    private val WS_DELIM = { t: JdkTree -> sourceMatching("\\s+") }
    private val COMMA_DELIM = { t: JdkTree -> sourceBefore(",") }
    private val SEMI_DELIM = { t: JdkTree -> sourceBefore(";") }
    private val NO_DELIM = { t: JdkTree -> "" }

    private val typeCache = TypeCache()

    private var endPosTable: EndPosTable by Delegates.notNull()
    private var cursor: Int = 0

    companion object {
        private val logger = LoggerFactory.getLogger(OracleJdkParserVisitor::class.java)
    }

    override fun visitAnnotation(node: AnnotationTree, fmt: Formatting.Reified): Tree {
        skip("@")
        val name = node.annotationType.convert<NameTree>()

        val args = if(node.arguments.size > 0) {
            val argsPrefix = sourceBefore("(")
            val args: List<Expression> = if (node.arguments.size == 1) {
                val arg = node.arguments[0] as JCTree.JCAssign
                listOf(if (arg.endPos() < 0) {
                    // this is the "value" argument, but without an explicit "value = ..."
                    arg.rhs.convert { sourceBefore(")") }
                } else {
                    // this is either an explicit "value" argument or is assigning some other property
                    arg.convert { sourceBefore(")") }
                })
            } else {
                node.arguments.convertAll(COMMA_DELIM, { sourceBefore(")") })
            }

            Tr.Annotation.Arguments(args, Formatting.Reified(argsPrefix))
        } else null

        return Tr.Annotation(name, args, node.type(), fmt)
    }

    override fun visitArrayAccess(node: ArrayAccessTree, fmt: Formatting.Reified): Tree {
        val indexed = node.expression.convert<Expression>()

        val dimensionPrefix = sourceBefore("[")
        val dimension = Tr.ArrayAccess.Dimension(node.index.convert<Expression> { sourceBefore("]") },
                Formatting.Reified(dimensionPrefix))

        return Tr.ArrayAccess(indexed, dimension, node.type(), fmt)
    }

    override fun visitAssignment(node: AssignmentTree, fmt: Formatting.Reified): Tree {
        val variable = node.variable.convert<NameTree> { sourceBefore("=") }
        val assignment = node.expression.convert<Expression>()
        return Tr.Assign(variable, assignment, node.type(), fmt)
    }

    override fun visitBinary(node: BinaryTree, fmt: Formatting.Reified): Tree {
        val left = node.leftOperand.convert<Expression>()

        val opPrefix = Formatting.Reified(sourceMatching("\\s+"))
        val op = when ((node as JCTree.JCBinary).tag) {
            JCTree.Tag.PLUS -> { skip("+"); Tr.Binary.Operator.Addition(opPrefix) }
            JCTree.Tag.MINUS -> { skip("-"); Tr.Binary.Operator.Subtraction(opPrefix) }
            JCTree.Tag.DIV -> { skip("/"); Tr.Binary.Operator.Division(opPrefix) }
            JCTree.Tag.MUL -> { skip("*"); Tr.Binary.Operator.Multiplication(opPrefix) }
            JCTree.Tag.MOD -> { skip("%"); Tr.Binary.Operator.Modulo(opPrefix) }
            JCTree.Tag.AND -> { skip("&&"); Tr.Binary.Operator.And(opPrefix) }
            JCTree.Tag.OR -> { skip("||"); Tr.Binary.Operator.Or(opPrefix) }
            JCTree.Tag.BITAND -> { skip("&"); Tr.Binary.Operator.BitAnd(opPrefix) }
            JCTree.Tag.BITOR -> { skip("|"); Tr.Binary.Operator.BitOr(opPrefix) }
            JCTree.Tag.BITXOR -> { skip("^"); Tr.Binary.Operator.BitXor(opPrefix) }
            JCTree.Tag.SL -> { skip("<<"); Tr.Binary.Operator.LeftShift(opPrefix) }
            JCTree.Tag.SR -> { skip(">>"); Tr.Binary.Operator.RightShift(opPrefix) }
            JCTree.Tag.USR -> { skip(">>>"); Tr.Binary.Operator.UnsignedRightShift(opPrefix) }
            JCTree.Tag.LT -> { skip("<"); Tr.Binary.Operator.LessThan(opPrefix) }
            JCTree.Tag.GT -> { skip(">"); Tr.Binary.Operator.GreaterThan(opPrefix) }
            JCTree.Tag.LE -> { skip("<="); Tr.Binary.Operator.LessThanOrEqual(opPrefix) }
            JCTree.Tag.GE -> { skip(">="); Tr.Binary.Operator.GreaterThanOrEqual(opPrefix) }
            JCTree.Tag.EQ -> { skip("=="); Tr.Binary.Operator.Equal(opPrefix) }
            JCTree.Tag.NE -> { skip("!="); Tr.Binary.Operator.NotEqual(opPrefix) }
            else -> throw IllegalArgumentException("Unexpected binary tag ${node.tag}")
        }

        val right = node.rightOperand.convert<Expression>()

        return Tr.Binary(left, op, right, node.type(), fmt)
    }

    override fun visitBlock(node: BlockTree, fmt: Formatting.Reified): Tree {
        val static = if((node as JCTree.JCBlock).flags and Flags.STATIC.toLong() != 0L) {
            skip("static")
            Tr.Empty(Formatting.Reified("", sourceBefore("{")))
        } else {
            skip("{")
            null
        }

        val statementDelim = { t: JdkTree ->
            sourceBefore(when(t) {
                is JCTree.JCLabeledStatement -> ""
                is JCTree.JCThrow -> ";"
                is JCTree.JCSynchronized -> ""
                is JCTree.JCBlock -> ""
                is JCTree.JCTry -> ""
                is JCTree.JCSkip -> ""
                is JCTree.JCWhileLoop -> ""
                is JCTree.JCIf -> ""
                is JCTree.JCForLoop -> ""
                is JCTree.JCBreak -> ";"
                is JCTree.JCSwitch -> ""
                is JCTree.JCAssert -> ";"
                is JCTree.JCContinue -> ";"
                is JCTree.JCExpressionStatement -> ";"
                is JCTree.JCReturn -> ";"
                is JCTree.JCCase -> ":"
                is JCTree.JCEnhancedForLoop -> ""
                is JCTree.JCClassDecl -> ""
                is JCTree.JCVariableDecl -> ";"
                is JCTree.JCDoWhileLoop -> ""
                else -> throw IllegalStateException("Unexpected statement type ${t.javaClass}")
            })
        }

        val statements = node
                .statements
                .filter {
                    // filter out synthetic super() invocations and the like
                    it.endPos() > 0
                }
                .convertAll<Statement>(statementDelim, statementDelim)

        return Tr.Block<Statement>(static, statements, fmt, sourceBefore("}"))
    }

    override fun visitBreak(node: BreakTree, fmt: Formatting.Reified): Tree {
        skip("break")
        val label = node.label?.toString()?.let { name ->
            val label = Tr.Ident(name, null, Formatting.Reified(sourceBefore(name)))
            skip(name)
            label
        }
        return Tr.Break(label, fmt)
    }

    override fun visitCase(node: CaseTree, fmt: Formatting.Reified): Tree {
        val pattern = node.expression.convertOrNull<Expression> { sourceBefore(":") } ?:
            Tr.Ident(skip("default")!!, null, Formatting.Reified(sourceBefore(":")))
        return Tr.Case(
                pattern,
                node.statements.convertAll(SEMI_DELIM, SEMI_DELIM),
                fmt
        )
    }

    override fun visitCatch(node: CatchTree, fmt: Formatting.Reified): Tree {
        skip("catch")

        val paramPrefix = sourceBefore("(")
        val paramDecl = node.parameter.convert<Tr.VariableDecl> { sourceBefore(")") }
        val param = Tr.Parentheses(paramDecl, Formatting.Reified(paramPrefix))

        return Tr.Catch(param, node.block.convert(), fmt)
    }

    override fun visitClass(node: ClassTree, fmt: Formatting.Reified): Tree {
        val annotations = node.modifiers.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)

        val modifiers = node.modifiers.flags.mapIndexed { i, mod ->
            val modPrefix = sourceMatching("\\s+")
            cursor += mod.name.length
            val modFormat = Formatting.Reified(modPrefix)
            when (mod) {
                Modifier.PUBLIC -> Tr.ClassDecl.Modifier.Public(modFormat)
                Modifier.PROTECTED -> Tr.ClassDecl.Modifier.Protected(modFormat)
                Modifier.PRIVATE -> Tr.ClassDecl.Modifier.Private(modFormat)
                Modifier.ABSTRACT -> Tr.ClassDecl.Modifier.Abstract(modFormat)
                Modifier.STATIC -> Tr.ClassDecl.Modifier.Static(modFormat)
                Modifier.FINAL -> Tr.ClassDecl.Modifier.Final(modFormat)
                else -> throw IllegalArgumentException("Unexpected modifier $mod")
            }
        }

        val kind = if(node.modifiers.hasFlag(Flags.ENUM)) {
            Tr.ClassDecl.Kind.Enum(Formatting.Reified(sourceBefore("enum")))
        } else if(node.modifiers.hasFlag(Flags.ANNOTATION)) {
            // note that annotations ALSO have the INTERFACE flag
            Tr.ClassDecl.Kind.Annotation(Formatting.Reified(sourceBefore("@interface")))
        } else if(node.modifiers.hasFlag(Flags.INTERFACE)) {
            Tr.ClassDecl.Kind.Interface(Formatting.Reified(sourceBefore("interface")))
        } else {
            Tr.ClassDecl.Kind.Class(Formatting.Reified(sourceBefore("class")))
        }

        val name = Tr.Ident((node as JCTree.JCClassDecl).simpleName.toString(), node.type(),
                Formatting.Reified(sourceBefore(node.simpleName.toString())))

        val typeParams = if(node.typeParameters.isNotEmpty()) {
            val genericPrefix = sourceBefore("<")
            Tr.TypeParameters(node.typeParameters.convertAll(COMMA_DELIM, { sourceBefore(">") }),
                    Formatting.Reified(genericPrefix))
        } else null

        val extends = node.extendsClause.convertOrNull<Tree>()
        val implements = node.implementsClause.convertAll<Tree>(COMMA_DELIM, NO_DELIM)

        val bodyPrefix = sourceBefore("{")

        // enum values are required by the grammar to occur before any ordinary field, constructor, or method members
        val enumValues = node.members
                .filterIsInstance<JCTree.JCVariableDecl>()
                .filter { it.modifiers.hasFlag(Flags.ENUM) }
                .convertAll<Tree>(COMMA_DELIM, {
                    // this semicolon is required when there are non-value members, but can still
                    // be present when there are not
                    sourceMatching("\\s*;").trimEnd(';')
                })

        val memberDelim = { t: JdkTree -> if(t is JCTree.JCVariableDecl) sourceBefore(";") else "" }
        val members = node.members
                // we don't care about the compiler-inserted default constructor,
                // since it will never be subject to refactoring
                .filter {
                    when(it) {
                        is JCTree.JCMethodDecl -> !it.modifiers.hasFlag(Flags.GENERATEDCONSTR)
                        is JCTree.JCVariableDecl -> !it.modifiers.hasFlag(Flags.ENUM)
                        else -> true
                    }
                }
                .convertAll<Tree>(memberDelim, memberDelim)

        val body = Tr.Block<Tree>(null, enumValues + members, Formatting.Reified(bodyPrefix), sourceBefore("}"))

        return Tr.ClassDecl(annotations, modifiers, kind, name, typeParams, extends, implements, body, node.type(), fmt)
    }

    override fun visitCompilationUnit(node: CompilationUnitTree, fmt: Formatting.Reified): Tree {
        endPosTable = (node as JCTree.JCCompilationUnit).endPositions
        cursor(node.startPosition)

        val packageDecl = if (node.packageName != null) {
            val packagePrefix = source.substring(0, node.startPosition)
            skip("package")
            val pkg = Tr.Package(node.packageName.convert(), Formatting.Reified(packagePrefix))
            skip(";")
            pkg
        } else null

        return Tr.CompilationUnit(
                SourceFile.fromText(path.toString(), source),
                packageDecl,
                node.imports.convertAll(SEMI_DELIM, SEMI_DELIM),
                node.typeDecls.filterIsInstance<JCTree.JCClassDecl>().convertAll(WS_DELIM, { source.substring(cursor) }),
                fmt
        )
    }

    override fun visitCompoundAssignment(node: CompoundAssignmentTree, fmt: Formatting.Reified): Tree {
        val left = (node as JCTree.JCAssignOp).lhs.convert<Expression>()

        val opPrefix = Formatting.Reified(sourceMatching("\\s+"))
        val op = when (node.tag) {
            JCTree.Tag.PLUS_ASG -> { skip("+="); Tr.AssignOp.Operator.Addition(opPrefix) }
            JCTree.Tag.MINUS_ASG -> { skip("-="); Tr.AssignOp.Operator.Subtraction(opPrefix) }
            JCTree.Tag.DIV_ASG -> { skip("/="); Tr.AssignOp.Operator.Division(opPrefix) }
            JCTree.Tag.MUL_ASG -> { skip("*="); Tr.AssignOp.Operator.Multiplication(opPrefix) }
            JCTree.Tag.MOD_ASG -> { skip("%="); Tr.AssignOp.Operator.Modulo(opPrefix) }
            JCTree.Tag.BITAND_ASG -> { skip("&="); Tr.AssignOp.Operator.BitAnd(opPrefix) }
            JCTree.Tag.BITOR_ASG -> { skip("|="); Tr.AssignOp.Operator.BitOr(opPrefix) }
            JCTree.Tag.BITXOR_ASG -> { skip("^="); Tr.AssignOp.Operator.BitXor(opPrefix) }
            JCTree.Tag.SL_ASG -> { skip("<<="); Tr.AssignOp.Operator.LeftShift(opPrefix) }
            JCTree.Tag.SR_ASG -> { skip(">>="); Tr.AssignOp.Operator.RightShift(opPrefix) }
            JCTree.Tag.USR_ASG -> { skip(">>>="); Tr.AssignOp.Operator.UnsignedRightShift(opPrefix) }
            else -> throw IllegalArgumentException("Unexpected compound assignment tag ${node.tag}")
        }

        return Tr.AssignOp(
                left,
                op,
                node.rhs.convert(),
                node.type(),
                fmt
        )
    }

    override fun visitConditionalExpression(node: ConditionalExpressionTree, fmt: Formatting.Reified): Tree {
        return Tr.Ternary(
                node.condition.convert(),
                node.trueExpression.convert(),
                node.falseExpression.convert(),
                node.type(),
                fmt
        )
    }

    override fun visitContinue(node: ContinueTree, fmt: Formatting.Reified): Tree {
        skip("continue")
        return Tr.Continue(
                node.label?.toString()?.let { lbl -> Tr.Ident(lbl, null, Formatting.Reified(sourceBefore(lbl))) },
                fmt
        )
    }

    override fun visitDoWhileLoop(node: DoWhileLoopTree, fmt: Formatting.Reified): Tree {
        skip("do")
        val stat = node.statement.convert<Statement> { sourceBefore("while") }
        return Tr.DoWhileLoop(
                stat,
                node.condition.convert(),
                fmt
        )
    }

    override fun visitEmptyStatement(node: EmptyStatementTree, fmt: Formatting.Reified): Tree {
        return Tr.Empty(fmt)
    }

    override fun visitEnhancedForLoop(node: EnhancedForLoopTree, fmt: Formatting.Reified): Tree {
        skip("for")
        val ctrlPrefix = sourceBefore("(")
        val variable = node.variable.convert<Tr.VariableDecl> { sourceBefore(":") }
        val expression = node.expression.convert<Expression> { sourceBefore(")") }

        return Tr.ForEachLoop(
                Tr.ForEachLoop.Control(variable, expression, Formatting.Reified(ctrlPrefix)),
                node.statement.convert(),
                fmt
        )
    }

    fun visitEnumVariable(node: VariableTree, fmt: Formatting.Reified): Tree {
        skip(node.name.toString())
        val name = Tr.Ident(node.name.toString(), node.type(), Formatting.Reified.Empty)

        val initPrefix = sourceMatching("\\s*\\(")
        val initializer = if(initPrefix.isNotEmpty()) {
            val args = (node.initializer as JCTree.JCNewClass).args.convertAll<Expression>(COMMA_DELIM, { sourceBefore(")") })
            if((node.initializer as JCTree.JCNewClass).args.isEmpty())
                skip(")")
            Tr.EnumValue.Arguments(args, Formatting.Reified(initPrefix.trimEnd('(')))
        } else null

        return Tr.EnumValue(name, initializer, fmt)
    }

    override fun visitForLoop(node: ForLoopTree, fmt: Formatting.Reified): Tree {
        skip("for")
        val ctrlPrefix = sourceBefore("(")

        fun List<JdkTree>.convertAllOrEmpty(innerSuffix: (JdkTree) -> String = { "" },
                                                    suffix: (JdkTree) -> String = { "" }): List<Statement> {
            return when (size) {
                0 -> listOf(Tr.Empty(Formatting.Reified("", suffix(object : JCTree.JCSkip() {}))))
                else -> mapIndexed { i, tree ->
                    tree.convert<Statement>(if (i == size - 1) suffix else innerSuffix)
                }
            }
        }

        val init = node.initializer.convertAllOrEmpty(COMMA_DELIM, SEMI_DELIM)
        val condition = node.condition.convertOrNull<Expression>(SEMI_DELIM) ?:
                Tr.Empty(Formatting.Reified("", sourceBefore(";")))
        val update = node.update.convertAllOrEmpty(COMMA_DELIM, { sourceBefore(")") })

        return Tr.ForLoop(
                Tr.ForLoop.Control(init, condition, update, Formatting.Reified(ctrlPrefix)),
                node.statement.convert(),
                fmt
        )
    }

    override fun visitIdentifier(node: IdentifierTree, fmt: Formatting.Reified): Tree {
        cursor += node.name.toString().length
        return Tr.Ident(node.name.toString(), node.type(), fmt)
    }

    override fun visitIf(node: IfTree, fmt: Formatting.Reified): Tree {
        skip("if")

        val ifPart = node.condition.convert<Tr.Parentheses<Expression>>()
        val then = node.thenStatement.convert<Statement>()

        val elsePart = if(node.elseStatement is JCTree.JCStatement) {
            val elsePrefix = sourceBefore("else")
            Tr.If.Else(node.elseStatement.convert<Statement>(), Formatting.Reified(elsePrefix))
        } else null

        return Tr.If(ifPart, then, elsePart, fmt)
    }

    override fun visitImport(node: ImportTree, fmt: Formatting.Reified): Tree {
        skip("import")
        skipPattern("\\s+static")
        return Tr.Import(node.qualifiedIdentifier.convert(), node.isStatic, fmt)
    }

    override fun visitInstanceOf(node: InstanceOfTree, fmt: Formatting.Reified): Tree {
        return Tr.InstanceOf(
                node.expression.convert<Expression> { sourceBefore("instanceof") },
                node.type.convert(),
                node.type(),
                fmt
        )
    }

    override fun visitLabeledStatement(node: LabeledStatementTree, fmt: Formatting.Reified): Tree {
        skip(node.label.toString())
        return Tr.Label(
                Tr.Ident(node.label.toString(), null, Formatting.Reified("", sourceBefore(":"))),
                node.statement.convert(),
                fmt
        )
    }

    override fun visitLambdaExpression(node: LambdaExpressionTree, fmt: Formatting.Reified): Tree {
        skip("(")
        return Tr.Lambda(
                node.parameters.convertAll(COMMA_DELIM, { sourceBefore(")") }),
                Tr.Lambda.Arrow(Formatting.Reified(sourceBefore("->"))),
                node.body.convert(),
                node.type(),
                fmt
        )
    }

    override fun visitLiteral(node: LiteralTree, fmt: Formatting.Reified): Tree {
        cursor(node.endPos())
        return Tr.Literal(
                (node as JCTree.JCLiteral).typetag.tag(),
                node.value,
                source[node.endPos()-1].isUpperCase(),
                node.type(),
                fmt
        )
    }

    override fun visitMemberSelect(node: MemberSelectTree, fmt: Formatting.Reified): Tree {
        val target = (node as JCTree.JCFieldAccess).selected.convert<Expression>()
        skip(".")
        val name = Tr.Ident(node.name.toString(), null, Formatting.Reified(sourceBefore(node.name.toString())))
        return Tr.FieldAccess(target, name, node.type(), fmt)
    }

    override fun visitMethodInvocation(node: MethodInvocationTree, fmt: Formatting.Reified): Tree {
        val jcSelect = (node as JCTree.JCMethodInvocation).methodSelect
        val methSymbol = when (jcSelect) {
            null -> null
            is JCTree.JCIdent -> jcSelect.sym
            is JCTree.JCFieldAccess -> jcSelect.sym
            else -> throw IllegalArgumentException("Unexpected method select type $this")
        }

        val jcMeth = node.meth
        val select = when(jcMeth) {
            is JCTree.JCFieldAccess -> jcMeth.selected.convert<Expression> { sourceBefore(".") }
            is JCTree.JCIdent -> null
            else -> throw IllegalStateException("Unexpected method select type ${jcMeth.javaClass}")
        }

        // generic type parameters can only exist on qualified targets
        val typeParams = if(node.typeargs.isNotEmpty()) {
            val genericPrefix = sourceBefore("<")
            val genericParams = node.typeargs.convertAll<NameTree>(COMMA_DELIM, { sourceBefore(">") })
            Tr.MethodInvocation.TypeParameters(genericParams, Formatting.Reified(genericPrefix))
        } else null

        val name = when(jcMeth) {
            is JCTree.JCFieldAccess ->  Tr.Ident(jcMeth.name.toString(), null, Formatting.Reified(sourceBefore(jcMeth.name.toString())))
            is JCTree.JCIdent -> jcMeth.convert<Tr.Ident>()
            else -> throw IllegalStateException("Unexpected method select type ${jcMeth.javaClass}")
        }

        val argsPrefix = sourceBefore("(")
        val args = Tr.MethodInvocation.Arguments(
                if(node.args.isEmpty()) {
                    listOf(Tr.Empty(Formatting.Reified(sourceBefore(")"))))
                } else {
                    node.args.convertExpressionsOrEmpty(COMMA_DELIM, { sourceBefore(")") })
                },
                Formatting.Reified(argsPrefix))

        return Tr.MethodInvocation(
                select,
                typeParams,
                name,
                args,
                methSymbol.type().asMethod(),
                jcSelect?.type.type().asMethod(),
                methSymbol?.owner?.type().asClass(),
                fmt
        )
    }

    override fun visitMethod(node: MethodTree, fmt: Formatting.Reified): Tree {
        logger.trace("Visiting method {}", node.name)

        val annotations = node.modifiers.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)
        val modifiers = node.modifiers.flags.mapIndexed { i, mod ->
            val modFormat = Formatting.Reified(sourceMatching("\\s+"))
            cursor += mod.name.length
            when(mod) {
                Modifier.DEFAULT -> Tr.MethodDecl.Modifier.Default(modFormat)
                Modifier.PUBLIC -> Tr.MethodDecl.Modifier.Public(modFormat)
                Modifier.PROTECTED -> Tr.MethodDecl.Modifier.Protected(modFormat)
                Modifier.PRIVATE -> Tr.MethodDecl.Modifier.Private(modFormat)
                Modifier.ABSTRACT -> Tr.MethodDecl.Modifier.Abstract(modFormat)
                Modifier.STATIC -> Tr.MethodDecl.Modifier.Static(modFormat)
                Modifier.FINAL -> Tr.MethodDecl.Modifier.Final(modFormat)
                else -> throw IllegalArgumentException("Unexpected modifier $mod")
            }
        }

        // see https://docs.oracle.com/javase/tutorial/java/generics/methods.html
        val typeParams = if(node.typeParameters.isNotEmpty()) {
            val genericPrefix = sourceBefore("<")
            Tr.TypeParameters(node.typeParameters.convertAll(COMMA_DELIM, { sourceBefore(">") }),
                    Formatting.Reified(genericPrefix))
        } else null

        val returnType = node.returnType.convertOrNull<TypeTree>()

        val name = if(node.name.toString() == "<init>") {
            val owner = ((node as JCTree.JCMethodDecl).sym.owner as Symbol.ClassSymbol).name.toString()
            val constructor = Tr.Ident(owner, null, Formatting.Reified(sourceBefore(owner)))
            skip(owner)
            constructor
        } else {
            Tr.Ident(node.name.toString(), null, Formatting.Reified(sourceBefore(node.name.toString())))
        }

        val paramFmt = Formatting.Reified(sourceBefore("("))
        val params = if(node.parameters.isNotEmpty()) {
            Tr.MethodDecl.Parameters(node.parameters.convertAll<Tr.VariableDecl>(COMMA_DELIM, { sourceBefore(")") }), paramFmt)
        } else {
            Tr.MethodDecl.Parameters(listOf(Tr.Empty(Formatting.Reified(sourceBefore(")")))), paramFmt)
        }


        val throws = if(node.throws.isNotEmpty()) {
            val throwsPrefix = sourceBefore("throws")
            Tr.MethodDecl.Throws(node.throws.convertAll<NameTree>(COMMA_DELIM, NO_DELIM), Formatting.Reified(throwsPrefix))
        } else null

        val body = node.body.convertOrNull<Tr.Block<Statement>>()

        return Tr.MethodDecl(
                annotations,
                modifiers,
                typeParams,
                returnType,
                name,
                params,
                throws,
                body,
                node.defaultValue.convertOrNull { sourceBefore(";") },
                fmt
        )
    }

    override fun visitNewArray(node: NewArrayTree, fmt: Formatting.Reified): Tree {
        skip("new")

        val jcVarType = (node as JCTree.JCNewArray).elemtype
        val typeExpr = when(jcVarType) {
            is JCTree.JCArrayTypeTree -> {
                // we'll capture the array dimensions in a bit, just convert the element type
                var elementType = jcVarType.elemtype
                while(elementType is JCTree.JCArrayTypeTree) {
                    elementType = elementType.elemtype
                }
                elementType.convertOrNull<TypeTree>()
            }
            else -> jcVarType.convertOrNull<TypeTree>()
        }

        val dimensions = if(node.dimensions.isNotEmpty()) {
            node.dimensions.mapIndexed { i, dim ->
                val dimensionPrefix = sourceBefore("[")
                Tr.NewArray.Dimension(dim.convert { sourceBefore("]") }, Formatting.Reified(dimensionPrefix,
                        if(i == node.dimensions.size - 1 && node.initializers != null) sourceBefore("}") else ""))
            }
        } else {
            val matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)\\]").matcher(source)
            val dimensions = ArrayList<Tr.NewArray.Dimension>()
            while(matcher.find(cursor)) {
                cursor(matcher.end())
                val ws = Tr.Empty(Formatting.Reified(matcher.group(2)))
                dimensions.add(Tr.NewArray.Dimension(ws, Formatting.Reified(matcher.group(1))))
            }
            dimensions
        }

        val initializer = if(node.initializers != null) {
            val initPrefix = sourceBefore("{")
            Tr.NewArray.Initializer(node.initializers.convertExpressionsOrEmpty({ sourceBefore(",") }, { sourceBefore("}") }),
                    Formatting.Reified(initPrefix))
        } else null

        return Tr.NewArray(typeExpr, dimensions, initializer, node.type(), fmt)
    }

    override fun visitNewClass(node: NewClassTree, fmt: Formatting.Reified): Tree {
        skip("new")
        val clazz = node.identifier.convert<TypeTree>()

        val argPrefix = sourceBefore("(")
        val args = Tr.NewClass.Arguments(
                node.arguments.convertExpressionsOrEmpty(COMMA_DELIM, { sourceBefore(")") }),
                Formatting.Reified(argPrefix))

        val body = node.classBody?.let {
            val bodyPrefix = sourceBefore("{")

            val members = it.members
                    // we don't care about the compiler-inserted default constructor,
                    // since it will never be subject to refactoring
                    .filter { it !is JCTree.JCMethodDecl || it.modifiers.flags and Flags.GENERATEDCONSTR == 0L }
                    .convertAll<Tree>(NO_DELIM, NO_DELIM)

            Tr.Block(null, members, Formatting.Reified(bodyPrefix), sourceBefore("}"))
        }

        return Tr.NewClass(clazz, args, body, (node as JCTree.JCNewClass).type.type(), fmt)
    }

    override fun visitParameterizedType(node: ParameterizedTypeTree, fmt: Formatting.Reified): Tree {
        val clazz = node.type.convert<NameTree>()

        val typeArgPrefix = sourceBefore("<")
        val typeArgs = if(node.typeArguments.isEmpty()) {
            // raw type, see http://docs.oracle.com/javase/tutorial/java/generics/rawTypes.html
            listOf(Tr.Empty(Formatting.Reified(sourceBefore(">"))))
        } else {
            node.typeArguments.convertAll<NameTree>(COMMA_DELIM, { sourceBefore(">") })
        }

        return Tr.ParameterizedType(
                clazz,
                Tr.ParameterizedType.TypeArguments(typeArgs, Formatting.Reified(typeArgPrefix)),
                fmt
        )
    }

    override fun visitParenthesized(node: ParenthesizedTree, fmt: Formatting.Reified): Tree {
        skip("(")
        return Tr.Parentheses<Expression>(node.expression.convert { sourceBefore(")") }, fmt)
    }

    override fun visitPrimitiveType(node: PrimitiveTypeTree, fmt: Formatting.Reified): Tree {
        cursor(node.endPos())
        return Tr.Primitive(when (node.primitiveTypeKind) {
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
    }

    override fun visitReturn(node: ReturnTree, fmt: Formatting.Reified): Tree {
        skip("return")
        return Tr.Return(node.expression.convertOrNull(), fmt)
    }

    override fun visitSwitch(node: SwitchTree, fmt: Formatting.Reified): Tree {
        skip("switch")
        val selector = node.expression.convert<Tr.Parentheses<Expression>>()

        val casePrefix = sourceBefore("{")
        val cases = node.cases.convertAll<Tr.Case>(NO_DELIM, NO_DELIM)

        return Tr.Switch(selector, Tr.Block(null, cases, Formatting.Reified(casePrefix), sourceBefore("}")), fmt)
    }

    override fun visitSynchronized(node: SynchronizedTree, fmt: Formatting.Reified): Tree {
        skip("synchronized")
        return Tr.Synchronized(
                node.expression.convert(),
                node.block.convert(),
                fmt
        )
    }

    override fun visitThrow(node: ThrowTree, fmt: Formatting.Reified): Tree {
        skip("throw")
        return Tr.Throw(node.expression.convert(), fmt)
    }

    override fun visitTry(node: TryTree, fmt: Formatting.Reified): Tree {
        skip("try")
        val resources = if(node.resources.isNotEmpty()) {
            val resourcesPrefix = sourceBefore("(")
            val decls = node.resources.convertAll<Tr.VariableDecl>(SEMI_DELIM, { sourceBefore(")") })
            Tr.Try.Resources(decls, Formatting.Reified(resourcesPrefix))
        } else null

        val block = node.block.convert<Tr.Block<Statement>>()
        val catches = node.catches.convertAll<Tr.Catch>(NO_DELIM, NO_DELIM)

        val finally = if(node.finallyBlock != null) {
            val finallyPrefix = sourceBefore("finally")
            Tr.Try.Finally(node.finallyBlock.convert<Tr.Block<Statement>>(),
                    Formatting.Reified(finallyPrefix))
        } else null

        return Tr.Try(resources, block, catches, finally, fmt)
    }

    override fun visitTypeCast(node: TypeCastTree, fmt: Formatting.Reified): Tree {
        val clazzPrefix = sourceBefore("(")
        val clazz = Tr.Parentheses(node.type.convert<TypeTree> { sourceBefore(")") },
                Formatting.Reified(clazzPrefix))

        return Tr.TypeCast(clazz, node.expression.convert(), fmt)
    }

    override fun visitTypeParameter(node: TypeParameterTree, fmt: Formatting.Reified): Tree {
        val annotations = node.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)
        val name = TreeBuilder.buildName(typeCache, node.name.toString(), Formatting.Reified(sourceBefore(node.name.toString())))

        // see https://docs.oracle.com/javase/tutorial/java/generics/bounded.html
        val bounds = node.bounds.convertAll<Expression>({ sourceBefore("&") }, NO_DELIM)

        return Tr.TypeParameter(annotations, name, bounds, fmt)
    }

    override fun visitUnionType(node: UnionTypeTree, fmt: Formatting.Reified): Tree {
        return Tr.MultiCatch(node.typeAlternatives.convertAll({ sourceBefore("|") }, NO_DELIM), fmt)
    }

    override fun visitUnary(node: UnaryTree, fmt: Formatting.Reified): Tree {
        val (op: Tr.Unary.Operator, expr: Expression) = when((node as JCTree.JCUnary).tag) {
            JCTree.Tag.POS -> {
                skip("+")
                Tr.Unary.Operator.Positive(Formatting.Reified.Empty) to node.arg.convert<Expression>()
            }
            JCTree.Tag.NEG -> {
                skip("-")
                Tr.Unary.Operator.Negative(Formatting.Reified.Empty) to node.arg.convert<Expression>()
            }
            JCTree.Tag.PREDEC -> {
                skip("--")
                Tr.Unary.Operator.PreDecrement(Formatting.Reified.Empty) to node.arg.convert<Expression>()
            }
            JCTree.Tag.PREINC -> {
                skip("++")
                Tr.Unary.Operator.PreIncrement(Formatting.Reified.Empty) to node.arg.convert<Expression>()
            }
            JCTree.Tag.POSTDEC -> {
                val expr = node.arg.convert<Expression>()
                Tr.Unary.Operator.PostDecrement(Formatting.Reified(sourceBefore("--"))) to expr
            }
            JCTree.Tag.POSTINC -> {
                val expr = node.arg.convert<Expression>()
                Tr.Unary.Operator.PostIncrement(Formatting.Reified(sourceBefore("++"))) to expr
            }
            JCTree.Tag.COMPL -> {
                skip("~")
                Tr.Unary.Operator.Complement(Formatting.Reified.Empty) to node.arg.convert<Expression>()
            }
            JCTree.Tag.NOT -> {
                skip("!")
                Tr.Unary.Operator.Not(Formatting.Reified.Empty) to node.arg.convert<Expression>()
            }
            else -> throw IllegalArgumentException("Unexpected unary tag ${node.tag}")
        }

        return Tr.Unary(op, expr, node.type(), fmt)
    }

    override fun visitVariable(node: VariableTree, fmt: Formatting.Reified): Tree? {
        logger.trace("Visiting variable {}", node.name.toString())

        if(node.name.toString() == "<error>") return null

        if(node.modifiers.hasFlag(Flags.ENUM)) {
            return visitEnumVariable(node, fmt)
        }

        val annotations = node.modifiers.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)

        val modifiers = if((node.modifiers as JCTree.JCModifiers).pos >= 0) {
            node.modifiers.flags.mapIndexed { i, mod ->
                val modFormat = Formatting.Reified(sourceMatching("\\s+"))
                cursor += mod.name.length
                when (mod) {
                    Modifier.PUBLIC -> Tr.VariableDecl.Modifier.Public(modFormat)
                    Modifier.PROTECTED -> Tr.VariableDecl.Modifier.Protected(modFormat)
                    Modifier.PRIVATE -> Tr.VariableDecl.Modifier.Private(modFormat)
                    Modifier.ABSTRACT -> Tr.VariableDecl.Modifier.Abstract(modFormat)
                    Modifier.STATIC -> Tr.VariableDecl.Modifier.Static(modFormat)
                    Modifier.FINAL -> Tr.VariableDecl.Modifier.Final(modFormat)
                    Modifier.TRANSIENT -> Tr.VariableDecl.Modifier.Transient(modFormat)
                    Modifier.VOLATILE -> Tr.VariableDecl.Modifier.Volatile(modFormat)
                    else -> throw IllegalArgumentException("Unexpected modifier $mod")
                }
            }
        } else {
            emptyList() // these are implicit modifiers, like "final" on try-with-resources variable declarations
        }

        val jcVarType = (node as JCTree.JCVariableDecl).vartype
        val varType = when(jcVarType) {
            is JCTree.JCArrayTypeTree -> {
                // we'll capture the array dimensions in a bit, just convert the element type
                var elementType = jcVarType.elemtype
                while(elementType is JCTree.JCArrayTypeTree) {
                    elementType = elementType.elemtype
                }
                elementType.convert<TypeTree>()
            }
            else -> jcVarType.convert<TypeTree>()
        }

        fun dimensions(): List<Tr.VariableDecl.Dimension> {
            val matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)\\]").matcher(source)
            val dimensions = ArrayList<Tr.VariableDecl.Dimension>()
            while(matcher.find(cursor)) {
                cursor(matcher.end())
                val ws = Tr.Empty(Formatting.Reified(matcher.group(2)))
                dimensions.add(Tr.VariableDecl.Dimension(ws, Formatting.Reified(matcher.group(1))))
            }
            return dimensions
        }

        val beforeDimensions = dimensions()

        val varargMatcher = Pattern.compile("(\\s*)\\.{3}").matcher(source.substring(node.vartype.startPosition, node.vartype.endPos()))
        val varargs = if(varargMatcher.find()) {
            skipPattern("(\\s*)\\.{3}")
            Tr.VariableDecl.Varargs(Formatting.Reified(varargMatcher.group(1)))
        } else null

        val name = Tr.Ident(node.name.toString(), node.type(), Formatting.Reified(sourceBefore(node.name.toString()),
                if(node.init is JCTree.JCExpression) sourceBefore("=") else ""))

        val afterDimensions = dimensions()

        return Tr.VariableDecl(
                annotations,
                modifiers,
                varType,
                varargs,
                beforeDimensions,
                name,
                afterDimensions,
                node.init.convertOrNull(),
                node.type(),
                fmt)
    }

    override fun visitWhileLoop(node: WhileLoopTree, fmt: Formatting.Reified): Tree {
        skip("while")
        return Tr.WhileLoop(
                node.condition.convert(),
                node.statement.convert(),
                fmt
        )
    }

    override fun visitWildcard(node: WildcardTree, fmt: Formatting.Reified): Tree {
        skip("?")

        val bound = when((node as JCTree.JCWildcard).kind.kind!!) {
            BoundKind.EXTENDS -> Tr.Wildcard.Bound.Extends(Formatting.Reified(sourceBefore("extends")))
            BoundKind.SUPER -> Tr.Wildcard.Bound.Super(Formatting.Reified(sourceBefore("super")))
            BoundKind.UNBOUND -> null
        }

        return Tr.Wildcard(bound, node.inner.convert<NameTree>(), fmt)
    }

    /**
     * --------------
     * Conversion utilities
     * --------------
     */

    @Suppress("UNCHECKED_CAST")
    private fun <T : Tree> JdkTree.convert(suffix: (JdkTree) -> String = { "" }): T {
        val prefix = source.substring(cursor, Math.max((this as JCTree).startPosition, cursor))
        cursor += prefix.length
        val t = scan(this, Formatting.Reified(prefix)) as T
        (t.formatting as Formatting.Reified).suffix = suffix(this)
        cursor(Math.max(this.endPos(), cursor)) // if there is a non-empty suffix, the cursor may have already moved past it
        return t
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Tree> JdkTree.convertOrNull(suffix: (JdkTree) -> String = { "" }): T? =
            if (this is JdkTree) convert<T>(suffix) else null

    private fun <T: Tree> List<JdkTree>.convertAll(innerSuffix: (JdkTree) -> String, suffix: (JdkTree) -> String): List<T> =
            mapIndexed { i, tree -> tree.convert<T>(if (i == size - 1) suffix else innerSuffix) }

    private fun List<JdkTree>.convertExpressionsOrEmpty(innerSuffix: (JdkTree) -> String = { "" },
                                                        suffix: (JdkTree) -> String = { "" }): List<Expression> {
        return if(this.isEmpty()) {
            listOf(Tr.Empty(Formatting.Reified.Empty))
        } else {
            mapIndexed { i, tree ->
                tree.convert<Expression>(if (i == size - 1) suffix else innerSuffix)
            }
        }
    }

    /**
     * --------------
     * Type conversion
     * --------------
     */

    private val allFlagsMask = Type.Var.Flags.values().map { it.value }.reduce { f1, f2 -> f1 or f2 }

    private fun Symbol?.type(stack: List<Any?> = emptyList()): Type? {
        return when (this) {
            is Symbol.ClassSymbol -> {
                val fields = (this.members_field?.elements ?: emptyList())
                        .filterIsInstance<Symbol.VarSymbol>()
                        .map {
                            Type.Var(
                                    it.name.toString(),
                                    it.type.type(stack.plus(this)),
                                    this.flags() or allFlagsMask
                            )
                        }

                Type.Class.build(typeCache, this.className(), fields, null)
            }
            is Symbol.PackageSymbol -> Type.Package.build(typeCache, this.fullname.toString())
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

    private fun JdkTree.type(): Type? = (this as JCTree).type.type()

    private fun TypeTag.tag(): Type.Tag {
        return when (this) {
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
            TypeTag.BOT -> Type.Tag.Null
            else -> throw IllegalArgumentException("Unknown type tag $this")
        }
    }

    /**
     * --------------
     * Other convenience utilities
     * --------------
     */

    private fun JdkTree.endPos(): Int = (this as JCTree).getEndPosition(endPosTable)

    private fun sourceBefore(untilDelim: String): String {
        val delimIndex = source.indexOf(untilDelim, startIndex = cursor)
        if(delimIndex < 0) {
            throw IllegalStateException("Expected to find a delimiter $untilDelim")
        }

        val prefix = source.substring(cursor, delimIndex)
        cursor += prefix.length + untilDelim.length // advance past the delimiter
        return prefix
    }

    private fun sourceMatching(untilPattern: String): String {
        val matcher = Pattern.compile("\\G$untilPattern").matcher(source)
        return if(matcher.find(cursor)) {
            cursor(matcher.end())
            matcher.group()
        }
        else {
            ""
        }
    }

    private fun skip(token: String?): String? {
        if(token == null)
            return null
        if(source.substring(cursor, cursor + token.length) == token)
            cursor += token.length
        return token
    }

    private fun skipPattern(pattern: String) {
        val matcher = Pattern.compile("\\G$pattern").matcher(source)
        if(matcher.find(cursor)) {
            cursor(matcher.end())
        }
    }

    // Only exists as a function to make it easier to debug unexpected cursor shifts
    private fun cursor(n: Int) {
        cursor = n
    }

    private fun ModifiersTree.hasFlag(flag: Number): Boolean =
            (this as JCTree.JCModifiers).flags and flag.toLong() != 0L

    /**
     * Because Flags.asModifierSet() only matches on certain flags... (debugging utility only)
     */
    @Suppress("unused")
    private fun ModifiersTree.listFlags(): List<String> = (this as JCTree.JCModifiers).flags.listFlags()

    private fun Number.listFlags(): List<String> {
        val allFlags = Flags::class.java.declaredFields
                .filter {
                    it.isAccessible = true
                    it.get(null) is Number && it.name.matches("[A-Z_]+".toRegex())
                }
                .map { it.name to it.get(null) as Number }

        return allFlags.fold(emptyList<String>()) { all, f ->
            if(f.second.toLong() and this.toLong() != 0L)
                all + f.first
            else all
        }
    }
}

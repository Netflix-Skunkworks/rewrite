package com.netflix.java.refactor.tree.oraclejdk

import com.netflix.java.refactor.tree.*
import com.sun.source.tree.*
import com.sun.source.util.TreeScanner
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.EndPosTable
import com.sun.tools.javac.tree.JCTree
import javax.lang.model.type.TypeKind
import kotlin.properties.Delegates

fun JCTree.JCCompilationUnit.toAst(): JRCompilationUnit =
        object : TreeScanner<JRTree, Unit>() {
            var endPosTable: EndPosTable by Delegates.notNull()
            var source: String by Delegates.notNull()

            override fun reduce(r1: JRTree?, r2: JRTree?) = r1 ?: r2

            @Suppress("UNCHECKED_CAST") private fun <T : JRTree> Tree.convert(): T = scan(this, null) as T
            @Suppress("UNCHECKED_CAST") private fun <T : JRTree> Tree?.convertOrNull(): T? =
                    if (this is Tree) scan(this, null) as T? else null

            private fun <T : JRTree> List<Tree>?.convert(): List<T> = if (this == null) emptyList()
            else map { it.convertOrNull<T>() }.filterNotNull()

            override fun visitCompilationUnit(node: CompilationUnitTree, p: Unit?): JRTree {
                val cu = node as JCTree.JCCompilationUnit
                endPosTable = cu.endPositions
                source = cu.sourcefile.getCharContent(true).toString()
                return JRCompilationUnit(
                        source,
                        cu.imports.convert(),
                        cu.typeDecls.filterIsInstance<JCTree.JCClassDecl>().convert(),
                        cu.posRange()
                )
            }

            override fun visitNewClass(node: NewClassTree, p: Unit?): JRTree =
                    JRNewClass(
                            node.enclosingExpression.convertOrNull(),
                            node.typeArguments.convert(),
                            node.identifier.convert(),
                            node.arguments.convert(),
                            node.classBody.convertOrNull(),
                            (node as JCTree.JCNewClass).type.jrType(),
                            node.posRange(),
                            node.source()
                    )

//                var r = scan(node.modifiers, p)
//                r = scanAndReduce(node.typeParameters, p, r)
            override fun visitClass(node: ClassTree, p: Unit?): JRTree =
                    JRClassDecl(
                            node.simpleName.toString(),
                            node.members.filterIsInstance<JCTree.JCVariableDecl>().convert(),
                            node.members.filterIsInstance<JCTree.JCMethodDecl>()
                                    // we don't care about the compiler-inserted default constructor, 
                                    // since it will never be subject to refactoring
                                    .filter { it.modifiers.flags and Flags.GENERATEDCONSTR == 0L }
                                    .convert(),
                            node.extendsClause.convertOrNull(),
                            node.implementsClause.convert(),
                            node.posRange()
                    )

            override fun visitMethodInvocation(node: MethodInvocationTree, p: Unit?): JRTree {
                val meth = node as JCTree.JCMethodInvocation
                val select = meth.methodSelect

                val methSymbol = when (select) {
                    null -> null
                    is JCTree.JCIdent -> select.sym
                    is JCTree.JCFieldAccess -> select.sym
                    else -> throw IllegalArgumentException("Unexpected method select type $this")
                }
                
                return JRMethodInvocation(
                        scan(meth.meth, null) as JRExpression,
                        meth.args.map { scan(it, null) as JRExpression },
                        methSymbol.jrType() as JRType.Method?,
                        select?.type.jrType() as JRType.Method?,
                        methSymbol?.owner?.jrType() as JRType.Class?,
                        meth.posRange(),
                        meth.source()
                )
            }

            override fun visitMethod(node: MethodTree, p: Unit?): JRTree =
                    JRMethodDecl(
                            node.name.toString(),
                            node.returnType.convertOrNull(), // only null when compilation problem (literally no return type)
                            node.parameters.convert(),
                            node.throws.convert(),
                            node.body.convert(),
                            node.defaultValue.convertOrNull(),
                            node.posRange()
                    )

            override fun visitBlock(node: BlockTree, p: Unit?): JRTree =
                    JRBlock(node.statements.convert(), node.posRange())

            override fun visitLiteral(node: LiteralTree, p: Unit?): JRTree {
                val literal = node as JCTree.JCLiteral
                return JRLiteral(literal.typetag.tag(),
                        node.value,
                        node.posRange(),
                        node.source())
            }

            override fun visitPrimitiveType(node: PrimitiveTypeTree, p: Unit?): JRTree =
                    JRPrimitive(when (node.primitiveTypeKind) {
                        TypeKind.BOOLEAN -> JRType.Tag.Boolean
                        TypeKind.BYTE -> JRType.Tag.Byte
                        TypeKind.CHAR -> JRType.Tag.Char
                        TypeKind.DOUBLE -> JRType.Tag.Double
                        TypeKind.FLOAT -> JRType.Tag.Float
                        TypeKind.INT -> JRType.Tag.Int
                        TypeKind.LONG -> JRType.Tag.Long
                        TypeKind.SHORT -> JRType.Tag.Short
                        TypeKind.VOID -> JRType.Tag.Void
                        else -> throw IllegalArgumentException("Unknown primitive type $this")
                    }, node.posRange())

            override fun visitVariable(node: VariableTree, p: Unit?): JRTree? = when (node.name.toString()) {
                "<error>" -> null
                else ->
                    JRVariableDecl(
                            node.name.toString(),
                            node.nameExpression.convertOrNull(),
                            (node as JCTree.JCVariableDecl).vartype.convertOrNull(),
                            node.init.convertOrNull(),
                            node.type.jrType(),
                            node.posRange()
                    )
            }

            override fun visitImport(node: ImportTree, p: Unit?): JRTree =
                    JRImport(scan(node.qualifiedIdentifier, null) as JRFieldAccess, node.posRange())

            override fun visitMemberSelect(node: MemberSelectTree, p: Unit?): JRTree {
                val fa = node as JCTree.JCFieldAccess
                return JRFieldAccess(
                        fa.name.toString(),
                        fa.selected.convert(),
                        fa.type.jrType(),
                        fa.posRange(),
                        fa.source()
                )
            }

            override fun visitIdentifier(node: IdentifierTree, p: Unit?): JRTree =
                    JRIdent(
                            node.name.toString(),
                            (node as JCTree.JCIdent).type.jrType(),
                            node.posRange(),
                            node.source()
                    )

            private fun Symbol?.jrType(): JRType? {
                val owner = { this?.owner?.jrType() }
                return when (this) {
                    is Symbol.ClassSymbol -> JRType.Class(this.className(), owner())
                    is Symbol.PackageSymbol -> JRType.Package(this.fullname.toString(), owner())
                    is Symbol.MethodSymbol -> {
                        when (this.type) {
                            is Type.ForAll -> (this.type as Type.ForAll).qtype.jrType()
                            else -> {
                                val params = this.params.map { it.jrType() }.filterNotNull()
                                JRType.Method(this.returnType.jrType(), params)
                            }
                        }
                    }
                    is Symbol.VarSymbol -> JRType.GenericTypeVariable(this.name.toString(), null)
                    else -> null
                }
            }

            private fun Type?.jrType(): JRType? {
                val owner = { this?.tsym?.owner.jrType() }
                return when (this) {
                    is Type.PackageType -> JRType.Package((this.tsym as Symbol.PackageSymbol).fullname.toString(), owner())
                    is Type.ClassType -> JRType.Class((this.tsym as Symbol.ClassSymbol).className(), owner())
                    is Type.MethodType -> {
                        // in the case of generic method parameters or return type, the types here are concretized relative to the call site
                        val returnType = this.restype?.jrType()
                        val args = this.argtypes.map { it.jrType() }.filterNotNull()
                        JRType.Method(returnType, args)
                    }
                    is Type.TypeVar -> JRType.GenericTypeVariable(this.tsym.name.toString(), this.bound.jrType().asClass())
                    is Type.JCPrimitiveType -> JRType.Primitive(this.tag.tag())
                    is Type.ArrayType -> JRType.Array(this.elemtype.jrType()!!)
                    else -> null
                }
            }

            private fun Tree.posRange(): IntRange = (this as JCTree).posRange()
            private fun JCTree.posRange(): IntRange =
                    if (getEndPosition(endPosTable) < 0)
                        0..0
                    else
                        startPosition..getEndPosition(endPosTable) - 1

            private fun Tree.source(): String = source.substring(posRange())

            private fun TypeTag.tag() = when (this) {
                TypeTag.BOOLEAN -> JRType.Tag.Boolean
                TypeTag.BYTE -> JRType.Tag.Byte
                TypeTag.CHAR -> JRType.Tag.Char
                TypeTag.DOUBLE -> JRType.Tag.Double
                TypeTag.FLOAT -> JRType.Tag.Float
                TypeTag.INT -> JRType.Tag.Int
                TypeTag.LONG -> JRType.Tag.Long
                TypeTag.SHORT -> JRType.Tag.Short
                TypeTag.VOID -> JRType.Tag.Void
                TypeTag.NONE -> JRType.Tag.None
                TypeTag.CLASS -> JRType.Tag.Class
                else -> throw IllegalArgumentException("Unknown type tag $this")
            }

        }.scan(this, null) as JRCompilationUnit
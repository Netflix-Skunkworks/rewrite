package com.netflix.java.refactor.ast

import kotlin.properties.Delegates

open class AstVisitor<R> {
    var cu: Tr.CompilationUnit by Delegates.notNull()
    lateinit var default: (Tree?) -> R
    
    constructor(default: R) {
        this.default = { default }
    }
    
    constructor(default: (Tree?) -> R) {
        this.default = default
    }
    
    /**
     * Some sensible defaults for reduce (boolean OR, list concatenation, or else just the value of r1).
     * Override if your particular visitor needs to reduce values in a different way.
     */
    @Suppress("UNUSED_PARAMETER", "UNCHECKED_CAST")
    open fun reduce(r1: R, r2: R): R = when(r1) {
        is Boolean -> (r1 || r2 as Boolean) as R
        is Iterable<*> -> r1.plus(r2 as Iterable<*>) as R
        else -> r1
    }

    var cursor: Cursor = Cursor.Empty
    
    open fun visit(tree: Tree?): R =
        if(tree != null) {
            cursor = cursor.plus(tree)
            tree.accept(this)
        } else default(tree)
    
    private fun R.andThen(nodes: Iterable<Tree>): R = reduce(visit(nodes), this)
    private fun R.andThen(node: Tree?): R = if(node != null) reduce(visit(node), this) else this
    
    fun visit(nodes: Iterable<Tree>?): R =
        nodes?.let {
            var r: R = default(null)
            var first = true
            for (node in nodes) {
                r = if (first) visit(node) else r.andThen(node)
                first = false
            }
            r
        } ?: default(null)

    open fun visitImport(import: Tr.Import): R = visit(import.qualid)
    
    open fun visitFieldAccess(field: Tr.FieldAccess): R = visit(field.target)

    open fun visitClassDecl(classDecl: Tr.ClassDecl): R = 
        visit(classDecl.definitions)
                .andThen(classDecl.implements)
                .andThen(classDecl.extends)
    
    open fun visitMethodInvocation(meth: Tr.MethodInvocation): R = 
        visit(meth.methodSelect).andThen(meth.args)

    open fun visitVariable(variable: Tr.VariableDecl): R =
        visit(variable.varType)
                .andThen(variable.name)
                .andThen(variable.initializer)

    open fun visitCompilationUnit(cu: Tr.CompilationUnit): R {
        this.cu = cu
        return reduce(
                visit(cu.imports)
                        .andThen(cu.packageDecl)
                        .andThen(cu.classDecls), 
                visitEnd()
        )
    }
    
    open fun visitIdentifier(ident: Tr.Ident): R = default(ident)
    
    open fun visitBlock(block: Tr.Block): R = visit(block.statements)

    open fun visitMethod(method: Tr.MethodDecl): R =
        visit(method.params)
                .andThen(method.returnTypeExpr)
                .andThen(method.thrown)
                .andThen(method.defaultValue)
                .andThen(method.body)

    open fun visitNewClass(newClass: Tr.NewClass): R =
            visit(newClass.encl)
                    .andThen(newClass.identifier)
                    .andThen(newClass.typeArgs)
                    .andThen(newClass.args)
                    .andThen(newClass.classBody)

    open fun visitPrimitive(primitive: Tr.Primitive): R = default(primitive)
    
    open fun visitLiteral(literal: Tr.Literal): R = default(literal)
    
    open fun visitBinary(binary: Tr.Binary): R = 
            visit(binary.left)
                    .andThen(binary.right)
    
    open fun visitUnary(unary: Tr.Unary): R = visit(unary.expr)
    
    open fun visitForLoop(forLoop: Tr.ForLoop) =
            visit(forLoop.control.init)
                    .andThen(forLoop.control.condition)
                    .andThen(forLoop.control.update)
                    .andThen(forLoop.body)
    
    open fun visitForEachLoop(forEachLoop: Tr.ForEachLoop): R =
            visit(forEachLoop.variable)
                    .andThen(forEachLoop.iterable)
                    .andThen(forEachLoop.body)
    
    open fun visitIf(iff: Tr.If): R =
            visit(iff.ifCondition)
                    .andThen(iff.thenPart)
                    .andThen(iff.elsePart)
    
    open fun visitTernary(ternary: Tr.Ternary): R =
            visit(ternary.condition)
                    .andThen(ternary.truePart)
                    .andThen(ternary.falsePart)
    
    open fun visitWhileLoop(whileLoop: Tr.WhileLoop): R =
            visit(whileLoop.condition)
                    .andThen(whileLoop.body)
    
    open fun visitDoWhileLoop(doWhileLoop: Tr.DoWhileLoop): R =
            visit(doWhileLoop.condition)
                    .andThen(doWhileLoop.body)
    
    open fun visitBreak(breakStatement: Tr.Break): R = default(breakStatement)

    open fun visitContinue(continueStatement: Tr.Continue): R = default(continueStatement)
    
    open fun visitLabel(label: Tr.Label): R = visit(label.statement)
    
    open fun visitReturn(retrn: Tr.Return): R = visit(retrn.expr)
    
    open fun visitCase(case: Tr.Case): R = 
            visit(case.pattern)
                    .andThen(case.statements)
    
    open fun visitSwitch(switch: Tr.Switch): R = 
            visit(switch.selector)
                    .andThen(switch.cases)
    
    open fun visitAssign(assign: Tr.Assign): R = 
            visit(assign.variable)
                    .andThen(assign.assignment)

    open fun visitAssignOp(assign: Tr.AssignOp): R = 
            visit(assign.variable)
                    .andThen(assign.assignment)
    
    open fun visitThrow(thrown: Tr.Throw): R = visit(thrown.expr)
    
    open fun visitTry(tryable: Tr.Try): R =
            visit(tryable.resources)
                .andThen(tryable.body)
                .andThen(tryable.catchers)
                .andThen(tryable.finally)
    
    open fun visitTypeParameter(typeParameter: Tr.TypeParameter): R =
            visit(typeParameter.bounds)
                .andThen(typeParameter.annotations)
    
    open fun visitCatch(catch: Tr.Catch): R = 
            visit(catch.param)
                    .andThen(catch.body)
    
    open fun visitSynchronized(synch: Tr.Synchronized): R = 
            visit(synch.lock)
                    .andThen(synch.body)
    
    open fun visitEmpty(empty: Tr.Empty): R = default(empty)
    
    open fun visitPackage(pkg: Tr.Package): R = visit(pkg.expr)
    
    open fun visitParentheses(parens: Tr.Parentheses): R = visit(parens.expr)
    
    open fun visitInstanceOf(instanceOf: Tr.InstanceOf): R = 
            visit(instanceOf.expr)
                    .andThen(instanceOf.clazz)
    
    open fun visitNewArray(newArray: Tr.NewArray): R =
            visit(newArray.typeExpr)
                    .andThen(newArray.dimensions)
                    .andThen(newArray.elements)
    
    open fun visitAnnotation(annotation: Tr.Annotation): R =
            visit(annotation.annotationType)
                    .andThen(annotation.args)
    
    open fun visitArrayAccess(arrayAccess: Tr.ArrayAccess): R =
            visit(arrayAccess.indexed)
                    .andThen(arrayAccess.index)
    
    open fun visitLambda(lambda: Tr.Lambda): R =
            visit(lambda.params)
                    .andThen(lambda.body)

    open fun visitEnd() = default(null)
}
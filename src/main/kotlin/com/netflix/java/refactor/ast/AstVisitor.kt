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

    fun visit(cu: Tr.CompilationUnit): R = visit(cu, Cursor.Empty)
    
    open fun visit(tree: Tree?, cursor: Cursor): R = if(tree != null) tree.accept(this, cursor.plus(tree)) else default(tree)
    
    private fun R.andThen(nodes: Iterable<Tree>, cursor: Cursor): R = reduce(visit(nodes, cursor), this)
    private fun R.andThen(node: Tree?, cursor: Cursor): R = if(node != null) reduce(visit(node, cursor), this) else this
    
    fun visit(nodes: Iterable<Tree>?, cursor: Cursor): R =
        nodes?.let {
            var r: R = default(null)
            var first = true
            for (node in nodes) {
                r = if (first) visit(node, cursor) else r.andThen(node, cursor)
                first = false
            }
            r
        } ?: default(null)

    open fun visitImport(import: Tr.Import, cursor: Cursor): R = 
            visit(import.importKeyword, cursor)
                .andThen(import.qualid, cursor)
    
    open fun visitSelect(field: Tr.FieldAccess, cursor: Cursor): R = visit(field.target, cursor)

    open fun visitClassDecl(classDecl: Tr.ClassDecl, cursor: Cursor): R = 
        visit(classDecl.fields, cursor)
                .andThen(classDecl.implements, cursor)
                .andThen(classDecl.extends, cursor)
                .andThen(classDecl.methods, cursor)
    
    open fun visitMethodInvocation(meth: Tr.MethodInvocation, cursor: Cursor): R = 
        visit(meth.methodSelect, cursor).andThen(meth.args, cursor)

    open fun visitVariable(variable: Tr.VariableDecl, cursor: Cursor): R =
        visit(variable.varType, cursor)
                .andThen(variable.nameExpr, cursor)
                .andThen(variable.initializer, cursor)

    open fun visitCompilationUnit(cu: Tr.CompilationUnit, cursor: Cursor): R {
        this.cu = cu
        return reduce(
                visit(cu.imports, cursor)
                        .andThen(cu.packageDecl, cursor)
                        .andThen(cu.classDecls, cursor), 
                visitEnd()
        )
    }
    
    open fun visitIdentifier(ident: Tr.Ident, cursor: Cursor): R = default(ident)
    
    open fun visitBlock(block: Tr.Block, cursor: Cursor): R = visit(block.statements, cursor)

    open fun visitMethod(method: Tr.MethodDecl, cursor: Cursor): R =
        visit(method.params, cursor)
                .andThen(method.returnTypeExpr, cursor)
                .andThen(method.thrown, cursor)
                .andThen(method.defaultValue, cursor)
                .andThen(method.body, cursor)

    open fun visitNewClass(newClass: Tr.NewClass, cursor: Cursor): R =
            visit(newClass.encl, cursor)
                    .andThen(newClass.identifier, cursor)
                    .andThen(newClass.typeargs, cursor)
                    .andThen(newClass.args, cursor)
                    .andThen(newClass.classBody, cursor)

    open fun visitPrimitive(primitive: Tr.Primitive, cursor: Cursor): R = default(primitive)
    
    open fun visitLiteral(literal: Tr.Literal, cursor: Cursor): R = default(literal)
    
    open fun visitBinary(binary: Tr.Binary, cursor: Cursor): R = 
            visit(binary.left, cursor)
                    .andThen(binary.right, cursor)
    
    open fun visitUnary(unary: Tr.Unary, cursor: Cursor): R = visit(unary.expr, cursor)
    
    open fun visitForLoop(forLoop: Tr.ForLoop, cursor: Cursor) =
            visit(forLoop.init, cursor)
                    .andThen(forLoop.condition, cursor)
                    .andThen(forLoop.update, cursor)
                    .andThen(forLoop.body, cursor)
    
    open fun visitForEachLoop(forEachLoop: Tr.ForEachLoop, cursor: Cursor): R =
            visit(forEachLoop.variable, cursor)
                    .andThen(forEachLoop.iterable, cursor)
                    .andThen(forEachLoop.body, cursor)
    
    open fun visitIf(iff: Tr.If, cursor: Cursor): R =
            visit(iff.ifCondition, cursor)
                    .andThen(iff.thenPart, cursor)
                    .andThen(iff.elsePart, cursor)
    
    open fun visitTernary(ternary: Tr.Ternary, cursor: Cursor): R =
            visit(ternary.condition, cursor)
                    .andThen(ternary.truePart, cursor)
                    .andThen(ternary.falsePart, cursor)
    
    open fun visitWhileLoop(whileLoop: Tr.WhileLoop, cursor: Cursor): R =
            visit(whileLoop.condition, cursor)
                    .andThen(whileLoop.body, cursor)
    
    open fun visitDoWhileLoop(doWhileLoop: Tr.DoWhileLoop, cursor: Cursor): R =
            visit(doWhileLoop.condition, cursor)
                    .andThen(doWhileLoop.body, cursor)
    
    open fun visitBreak(breakStatement: Tr.Break, cursor: Cursor): R = default(breakStatement)

    open fun visitContinue(continueStatement: Tr.Continue, cursor: Cursor): R = default(continueStatement)
    
    open fun visitLabel(label: Tr.Label, cursor: Cursor): R = visit(label.statement, cursor)
    
    open fun visitReturn(retrn: Tr.Return, cursor: Cursor): R = visit(retrn.expr, cursor)
    
    open fun visitCase(case: Tr.Case, cursor: Cursor): R = 
            visit(case.pattern, cursor)
                    .andThen(case.statements, cursor)
    
    open fun visitSwitch(switch: Tr.Switch, cursor: Cursor): R = 
            visit(switch.selector, cursor)
                    .andThen(switch.cases, cursor)
    
    open fun visitAssign(assign: Tr.Assign, cursor: Cursor): R = 
            visit(assign.variable, cursor)
                    .andThen(assign.assignment, cursor)

    open fun visitAssignOp(assign: Tr.AssignOp, cursor: Cursor): R = 
            visit(assign.variable, cursor)
                    .andThen(assign.assignment, cursor)
    
    open fun visitThrow(thrown: Tr.Throw, cursor: Cursor): R = visit(thrown.expr, cursor)
    
    open fun visitTry(tryable: Tr.Try, cursor: Cursor): R =
            visit(tryable.resources, cursor)
                .andThen(tryable.body, cursor)
                .andThen(tryable.catchers, cursor)
                .andThen(tryable.finally, cursor)
    
    open fun visitCatch(catch: Tr.Catch, cursor: Cursor): R = 
            visit(catch.param, cursor)
                    .andThen(catch.body, cursor)
    
    open fun visitSynchronized(synch: Tr.Synchronized, cursor: Cursor): R = 
            visit(synch.lock, cursor)
                    .andThen(synch.body, cursor)
    
    open fun visitEmpty(empty: Tr.Empty, cursor: Cursor): R = default(empty)
    
    open fun visitParentheses(parens: Tr.Parentheses, cursor: Cursor): R = visit(parens.expr, cursor)
    
    open fun visitInstanceOf(instanceOf: Tr.InstanceOf, cursor: Cursor): R = 
            visit(instanceOf.expr, cursor)
                    .andThen(instanceOf.clazz, cursor)
    
    open fun visitNewArray(newArray: Tr.NewArray, cursor: Cursor): R =
            visit(newArray.typeExpr, cursor)
                    .andThen(newArray.dimensions, cursor)
                    .andThen(newArray.elements, cursor)
    
    open fun visitArrayAccess(arrayAccess: Tr.ArrayAccess, cursor: Cursor): R =
            visit(arrayAccess.indexed, cursor)
                    .andThen(arrayAccess.index, cursor)
    
    open fun visitLambda(lambda: Tr.Lambda, cursor: Cursor): R =
            visit(lambda.params, cursor)
                    .andThen(lambda.body, cursor)

    open fun visitEnd() = default(null)
}
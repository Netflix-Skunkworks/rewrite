package com.netflix.java.refactor.tree

import sun.tools.tree.BreakStatement

open class JRTreeVisitor<R>(val default: R) {
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

    fun scan(tree: JRTree?): R = if(tree != null) tree.accept(this) else default
    
    private fun R.andThen(nodes: Iterable<JRTree>): R = reduce(scan(nodes), this)
    private fun R.andThen(node: JRTree?): R = if(node != null) reduce(scan(node), this) else this
    
    fun scan(nodes: Iterable<JRTree>?): R =
        nodes?.let {
            var r: R = default
            var first = true
            for (node in nodes) {
                r = if (first) scan(node) else r.andThen(node)
                first = false
            }
            r
        } ?: default

    open fun visitImport(import: JRImport): R = scan(import.qualid)
    
    open fun visitSelect(field: JRFieldAccess): R = scan(field.target)

//        val r = scan(tree.mods)
//        scan(tree.typarams)
//        scan(tree.extending)
//        scan(tree.implementing)
//        scan(tree.defs)
    open fun visitClassDecl(classDecl: JRClassDecl): R = 
        scan(classDecl.fields)
                .andThen(classDecl.methods)
    
    open fun visitMethodInvocation(meth: JRMethodInvocation): R = 
        scan(meth.methodSelect)
//                .andThen(meth.typeargs)
                .andThen(meth.args)

//        var r = scan(node.getModifiers(), p)
//        r = scanAndReduce(node.getType(), p)
    open fun visitVariable(variable: JRVariableDecl): R =
        scan(variable.varType)
                .andThen(variable.nameExpr)
                .andThen(variable.initializer)

//        var r = scan(node.getPackageAnnotations(), p)
//        r = scanAndReduce(node.getPackageName(), p)
    open fun visitCompilationUnit(cu: JRCompilationUnit): R =
        scan(cu.imports)
                .andThen(cu.classDecls)
    
    open fun visitIdentifier(ident: JRIdent): R = default
    
    open fun visitBlock(block: JRBlock): R = scan(block.statements)

//        var r = scan(node.getModifiers(), p)
//        r = scanAndReduce(node.getTypeParameters(), p)
//        r = scanAndReduce(node.getParameters(), p)
//        r = scanAndReduce(node.getReceiverParameter(), p)
    open fun visitMethod(method: JRMethodDecl): R =
        scan(method.params)
                .andThen(method.returnTypeExpr)
                .andThen(method.thrown)
                .andThen(method.defaultValue)
                .andThen(method.body)

    open fun visitNewClass(newClass: JRNewClass): R =
            scan(newClass.encl)
                    .andThen(newClass.identifier)
                    .andThen(newClass.typeargs)
                    .andThen(newClass.args)
                    .andThen(newClass.classBody)

    open fun visitPrimitive(primitive: JRPrimitive): R = default
    
    open fun visitLiteral(literal: JRLiteral): R = default
    
    open fun visitBinary(binary: JRBinary): R =
            scan(binary.left).andThen(binary.right)
    
    open fun visitUnary(unary: JRUnary): R = scan(unary.expr)
    
    open fun visitForLoop(forLoop: JRForLoop): R =
            scan(forLoop.init)
                    .andThen(forLoop.condition)
                    .andThen(forLoop.update)
                    .andThen(forLoop.body)
    
    open fun visitForEachLoop(forEachLoop: JRForEachLoop): R =
            scan(forEachLoop.variable)
                    .andThen(forEachLoop.iterable)
                    .andThen(forEachLoop.body)
    
    open fun visitIf(iff: JRIf): R =
            scan(iff.ifCondition)
                    .andThen(iff.thenPart)
                    .andThen(iff.elsePart)
    
    open fun visitTernary(ternary: JRTernary): R =
            scan(ternary.condition)
                    .andThen(ternary.truePart)
                    .andThen(ternary.falsePart)
    
    open fun visitWhileLoop(whileLoop: JRWhileLoop): R =
            scan(whileLoop.condition)
                    .andThen(whileLoop.body)
    
    open fun visitDoWhileLoop(doWhileLoop: JRDoWhileLoop): R =
            scan(doWhileLoop.condition)
                    .andThen(doWhileLoop.body)
    
    open fun visitBreak(breakStatement: JRBreak): R = default

    open fun visitContinue(continueStatement: JRContinue): R = default
    
    open fun visitLabel(label: JRLabel): R = scan(label.statement)
    
    open fun visitReturn(retrn: JRReturn): R = scan(retrn.expr)
    
    open fun visitCase(case: JRCase): R = 
            scan(case.pattern)
                .andThen(case.statements)
    
    open fun visitSwitch(switch: JRSwitch): R = scan(switch.selector).andThen(switch.cases)
    
    open fun visitAssign(assign: JRAssign): R = scan(assign.variable).andThen(assign.assignment)
    
    open fun visitThrow(thrown: JRThrow): R = scan(thrown.expr)
    
    open fun visitTry(tryable: JRTry): R =
            scan(tryable.resources)
                .andThen(tryable.body)
                .andThen(tryable.catchers)
                .andThen(tryable.finally)
    
    open fun visitCatch(catch: JRCatch): R =
            scan(catch.param)
                .andThen(catch.body)
}
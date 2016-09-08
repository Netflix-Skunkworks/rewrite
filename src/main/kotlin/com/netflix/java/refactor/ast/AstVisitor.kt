package com.netflix.java.refactor.ast

import kotlin.properties.Delegates

open class AstVisitor<R>(val default: R) {
    var cu: CompilationUnit by Delegates.notNull()
    
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

    fun visit(tree: Tree?): R = if(tree != null) tree.accept(this) else default
    
    private fun R.andThen(nodes: Iterable<Tree>): R = reduce(visit(nodes), this)
    private fun R.andThen(node: Tree?): R = if(node != null) reduce(visit(node), this) else this
    
    fun visit(nodes: Iterable<Tree>?): R =
        nodes?.let {
            var r: R = default
            var first = true
            for (node in nodes) {
                r = if (first) visit(node) else r.andThen(node)
                first = false
            }
            r
        } ?: default

    open fun visitImport(import: Import): R = visit(import.qualid)
    
    open fun visitSelect(field: FieldAccess): R = visit(field.target)

//        val r = scan(tree.mods)
//        scan(tree.typarams)
//        scan(tree.extending)
//        scan(tree.implementing)
//        scan(tree.defs)
    open fun visitClassDecl(classDecl: ClassDecl): R = 
        visit(classDecl.fields)
                .andThen(classDecl.methods)
    
    open fun visitMethodInvocation(meth: MethodInvocation): R = 
        visit(meth.methodSelect)
//                .andThen(meth.typeargs)
                .andThen(meth.args)

//        var r = scan(node.getModifiers(), p)
//        r = scanAndReduce(node.getType(), p)
    open fun visitVariable(variable: VariableDecl): R =
        visit(variable.varType)
                .andThen(variable.nameExpr)
                .andThen(variable.initializer)

//        var r = scan(node.getPackageAnnotations(), p)
//        r = scanAndReduce(node.getPackageName(), p)
    open fun visitCompilationUnit(cu: CompilationUnit): R {
        this.cu = cu
        return reduce(visit(cu.imports).andThen(cu.classDecls), visitEnd())
    }
    
    open fun visitIdentifier(ident: Ident): R = default
    
    open fun visitBlock(block: Block): R = visit(block.statements)

//        var r = scan(node.getModifiers(), p)
//        r = scanAndReduce(node.getTypeParameters(), p)
//        r = scanAndReduce(node.getParameters(), p)
//        r = scanAndReduce(node.getReceiverParameter(), p)
    open fun visitMethod(method: MethodDecl): R =
        visit(method.params)
                .andThen(method.returnTypeExpr)
                .andThen(method.thrown)
                .andThen(method.defaultValue)
                .andThen(method.body)

    open fun visitNewClass(newClass: NewClass): R =
            visit(newClass.encl)
                    .andThen(newClass.identifier)
                    .andThen(newClass.typeargs)
                    .andThen(newClass.args)
                    .andThen(newClass.classBody)

    open fun visitPrimitive(primitive: Primitive): R = default
    
    open fun visitLiteral(literal: Literal): R = default
    
    open fun visitBinary(binary: Binary): R = visit(binary.left).andThen(binary.right)
    
    open fun visitUnary(unary: Unary): R = visit(unary.expr)
    
    open fun visitForLoop(forLoop: ForLoop): R =
            visit(forLoop.init)
                    .andThen(forLoop.condition)
                    .andThen(forLoop.update)
                    .andThen(forLoop.body)
    
    open fun visitForEachLoop(forEachLoop: ForEachLoop): R =
            visit(forEachLoop.variable)
                    .andThen(forEachLoop.iterable)
                    .andThen(forEachLoop.body)
    
    open fun visitIf(iff: If): R =
            visit(iff.ifCondition)
                    .andThen(iff.thenPart)
                    .andThen(iff.elsePart)
    
    open fun visitTernary(ternary: Ternary): R =
            visit(ternary.condition)
                    .andThen(ternary.truePart)
                    .andThen(ternary.falsePart)
    
    open fun visitWhileLoop(whileLoop: WhileLoop): R =
            visit(whileLoop.condition)
                    .andThen(whileLoop.body)
    
    open fun visitDoWhileLoop(doWhileLoop: DoWhileLoop): R =
            visit(doWhileLoop.condition)
                    .andThen(doWhileLoop.body)
    
    open fun visitBreak(breakStatement: Break): R = default

    open fun visitContinue(continueStatement: Continue): R = default
    
    open fun visitLabel(label: Label): R = visit(label.statement)
    
    open fun visitReturn(retrn: Return): R = visit(retrn.expr)
    
    open fun visitCase(case: Case): R = visit(case.pattern).andThen(case.statements)
    
    open fun visitSwitch(switch: Switch): R = visit(switch.selector).andThen(switch.cases)
    
    open fun visitAssign(assign: Assign): R = visit(assign.variable).andThen(assign.assignment)

    open fun visitAssignOp(assign: AssignOp): R = visit(assign.variable).andThen(assign.assignment)
    
    open fun visitThrow(thrown: Throw): R = visit(thrown.expr)
    
    open fun visitTry(tryable: Try): R =
            visit(tryable.resources)
                .andThen(tryable.body)
                .andThen(tryable.catchers)
                .andThen(tryable.finally)
    
    open fun visitCatch(catch: Catch): R = visit(catch.param).andThen(catch.body)
    
    open fun visitSynchronized(synch: Synchronized): R = visit(synch.lock).andThen(synch.body)
    
    open fun visitEmpty(empty: Empty): R = default
    
    open fun visitParentheses(parens: Parentheses): R = visit(parens.expr)
    
    open fun visitInstanceOf(instanceOf: InstanceOf): R = visit(instanceOf.expr).andThen(instanceOf.clazz)
    
    open fun visitNewArray(newArray: NewArray): R =
            visit(newArray.typeExpr)
                    .andThen(newArray.dimensions)
                    .andThen(newArray.elements)
    
    open fun visitArrayAccess(arrayAccess: ArrayAccess): R =
            visit(arrayAccess.indexed)
                    .andThen(arrayAccess.index)
    
    open fun visitLambda(lambda: Lambda): R =
            visit(lambda.params)
                    .andThen(lambda.body)

    open fun visitEnd() = default
    
    fun Tree.source() = cu.source.text.substring(pos)
}
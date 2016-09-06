package com.netflix.java.refactor.tree

data class JRForEachLoop(val variable: JRVariableDecl,
                    val iterable: JRExpression,
                    val body: JRStatement,
                    override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitForEachLoop(this)
}
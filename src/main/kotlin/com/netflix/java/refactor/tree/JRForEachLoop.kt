package com.netflix.java.refactor.tree

class JRForEachLoop(val variable: JRVariableDecl,
                    val iterable: JRExpression,
                    val body: JRStatement,
                    val type: JRType?,
                    override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitForEachLoop(this)
}
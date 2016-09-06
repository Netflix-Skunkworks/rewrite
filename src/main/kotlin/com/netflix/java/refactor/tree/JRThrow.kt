package com.netflix.java.refactor.tree

data class JRThrow(val expr: JRExpression,
                   override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitThrow(this)
}
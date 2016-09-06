package com.netflix.java.refactor.tree

data class JRArrayAccess(val indexed: JRExpression, 
                         val index: JRExpression,
                         override val pos: IntRange,
                         override val source: String): JRExpression {
 
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitArrayAccess(this)
}
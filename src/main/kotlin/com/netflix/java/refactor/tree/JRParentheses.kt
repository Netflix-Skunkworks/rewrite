package com.netflix.java.refactor.tree

data class JRParentheses(val expr: JRExpression,
                         override val pos: IntRange,
                         override val source: String): JRExpression {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitParentheses(this)
}
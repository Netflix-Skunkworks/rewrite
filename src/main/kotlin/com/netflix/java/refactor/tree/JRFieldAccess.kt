package com.netflix.java.refactor.tree

data class JRFieldAccess(val fieldName: String,
                         val target: JRExpression,
                         val type: JRType?,
                         override val pos: IntRange,
                         override val source: String): JRExpression {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitSelect(this)
}
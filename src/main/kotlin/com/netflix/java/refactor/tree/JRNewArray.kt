package com.netflix.java.refactor.tree


data class JRNewArray(val type: JRExpression,
                      val dimensions: List<JRExpression>,
                      val elements: List<JRExpression>,
                      override val pos: IntRange,
                      override val source: String): JRExpression {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitNewArray(this)
}
package com.netflix.java.refactor.tree

data class JRCase(val pattern: JRExpression?, // null for the default case
                  val statements: List<JRStatement>,
                  override val pos: IntRange): JRStatement {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitCase(this)
}
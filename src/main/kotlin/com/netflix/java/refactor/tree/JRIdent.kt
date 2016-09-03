package com.netflix.java.refactor.tree

data class JRIdent(val name: String, 
                   val type: JRType?,
                   override val pos: IntRange,
                   override val source: String): JRExpression {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitIdentifier(this)
}
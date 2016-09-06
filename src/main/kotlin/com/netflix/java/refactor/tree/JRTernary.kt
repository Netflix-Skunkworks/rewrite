package com.netflix.java.refactor.tree

data class JRTernary(val condition: JRExpression,
                val truePart: JRExpression,
                val falsePart: JRExpression,
                val type: JRType?,
                override val pos: IntRange,
                override val source: String): JRExpression {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitTernary(this)
}
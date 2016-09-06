package com.netflix.java.refactor.tree

data class JRIf(val ifCondition: JRExpression,
                val thenPart: JRStatement,
                val elsePart: JRStatement?,
                val type: JRType?,
                override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitIf(this)
}
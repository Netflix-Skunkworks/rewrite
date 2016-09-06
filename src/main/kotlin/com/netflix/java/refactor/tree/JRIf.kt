package com.netflix.java.refactor.tree

data class JRIf(val ifCondition: JRParentheses,
                val thenPart: JRStatement,
                val elsePart: JRStatement?,
                override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitIf(this)
    
}
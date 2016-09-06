package com.netflix.java.refactor.tree

data class JRAssign(val variable: JRExpression,
                    val assignment: JRExpression,
                    override val pos: IntRange, 
                    override val source: String): JRExpression, JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitAssign(this)
}
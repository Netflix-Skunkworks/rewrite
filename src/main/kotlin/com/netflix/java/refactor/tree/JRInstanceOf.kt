package com.netflix.java.refactor.tree

data class JRInstanceOf(val expr: JRExpression,
                        val clazz: JRTree,
                        override val pos: IntRange,
                        override val source: String): JRExpression {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitInstanceOf(this) 
}
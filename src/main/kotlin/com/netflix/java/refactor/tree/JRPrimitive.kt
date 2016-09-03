package com.netflix.java.refactor.tree

data class JRPrimitive(val typeTag: JRType.Tag,
                       override val pos: IntRange): JRExpression {
    override val source = typeTag.name
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitPrimitive(this)
}
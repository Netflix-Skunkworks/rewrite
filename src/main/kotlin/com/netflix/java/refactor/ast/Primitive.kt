package com.netflix.java.refactor.ast

data class Primitive(val typeTag: Type.Tag,
                     override val type: Type?,
                     override val pos: IntRange): Expression {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitPrimitive(this)
}
package com.netflix.java.refactor.ast

data class ArrayAccess(val indexed: Expression,
                       val index: Expression,
                       override val type: Type?,
                       override val pos: IntRange): Expression {
 
    override fun <R> accept(v: AstVisitor<R>): R = v.visitArrayAccess(this)
}
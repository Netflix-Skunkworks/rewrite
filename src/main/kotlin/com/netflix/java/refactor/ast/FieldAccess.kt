package com.netflix.java.refactor.ast

data class FieldAccess(val fieldName: String,
                       val target: Expression,
                       override val type: Type?,
                       override val pos: IntRange): Expression {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitSelect(this)
}
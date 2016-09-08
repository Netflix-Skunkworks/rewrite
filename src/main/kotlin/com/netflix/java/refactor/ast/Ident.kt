package com.netflix.java.refactor.ast

data class Ident(val name: String,
                 override val type: Type?,
                 override val pos: IntRange): Expression {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitIdentifier(this)
}
package com.netflix.java.refactor.ast

data class Throw(val expr: Expression,
                 override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitThrow(this)
}
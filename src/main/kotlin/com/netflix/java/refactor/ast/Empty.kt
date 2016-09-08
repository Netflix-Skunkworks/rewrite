package com.netflix.java.refactor.ast

data class Empty(override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitEmpty(this)
}
package com.netflix.java.refactor.ast

data class Return(val expr: Expression?,
                  override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitReturn(this)
}
package com.netflix.java.refactor.ast

data class Break(val label: String?,
                 override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitBreak(this)
}
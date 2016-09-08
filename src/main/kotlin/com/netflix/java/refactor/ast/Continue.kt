package com.netflix.java.refactor.ast

data class Continue(val label: String?,
                    override val pos: IntRange): Statement {

    override fun <R> accept(v: AstVisitor<R>): R = v.visitContinue(this)
}
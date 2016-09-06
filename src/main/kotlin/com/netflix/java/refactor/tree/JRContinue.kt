package com.netflix.java.refactor.tree

data class JRContinue(val label: String?,
                   override val pos: IntRange): JRStatement {

    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitContinue(this)
}
package com.netflix.java.refactor.tree

data class JREmpty(override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitEmpty(this)
}
package com.netflix.java.refactor.tree

data class JRSynchronized(val lock: JRExpression, 
                          val body: JRBlock,
                          override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitSynchronized(this)
}

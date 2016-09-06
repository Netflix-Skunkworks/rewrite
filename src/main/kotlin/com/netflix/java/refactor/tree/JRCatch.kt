package com.netflix.java.refactor.tree

data class JRCatch(val param: JRVariableDecl,
                   val body: JRBlock,
                   override val pos: IntRange): JRTree {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitCatch(this)
}
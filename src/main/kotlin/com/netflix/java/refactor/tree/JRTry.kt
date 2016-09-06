package com.netflix.java.refactor.tree

data class JRTry(val resources: List<JRVariableDecl>, 
                 val body: JRBlock,
                 val catchers: List<JRCatch>,
                 val finally: JRBlock?,
                 override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitTry(this)
}
package com.netflix.java.refactor.tree

data class JRSwitch(val selector: JRExpression, 
               val cases: List<JRCase>,
               override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitSwitch(this)
}
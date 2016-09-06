package com.netflix.java.refactor.tree


data class JRForLoop(val init: List<JRStatement>,
                val condition: JRExpression?,
                val update: List<JRStatement>,
                val body: JRStatement,
                val type: JRType?,
                override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitForLoop(this)
}
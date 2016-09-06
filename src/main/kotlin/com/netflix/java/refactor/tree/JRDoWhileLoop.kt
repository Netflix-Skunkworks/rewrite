package com.netflix.java.refactor.tree

class JRDoWhileLoop(val condition: JRExpression,
                    val body: JRStatement,
                    val type: JRType?,
                    override val pos: IntRange) : JRStatement {

    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitDoWhileLoop(this)
}
package com.netflix.java.refactor.tree

data class JRDoWhileLoop(val condition: JRParentheses,
                    val body: JRStatement,
                    override val pos: IntRange) : JRStatement {

    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitDoWhileLoop(this)
}
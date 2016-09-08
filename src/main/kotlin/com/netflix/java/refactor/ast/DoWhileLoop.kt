package com.netflix.java.refactor.ast

data class DoWhileLoop(val condition: Parentheses,
                       val body: Statement,
                       override val pos: IntRange) : Statement {

    override fun <R> accept(v: AstVisitor<R>): R = v.visitDoWhileLoop(this)
}
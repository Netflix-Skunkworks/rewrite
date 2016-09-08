package com.netflix.java.refactor.ast

data class Parentheses(val expr: Expression,
                       override val type: Type?,
                       override val pos: IntRange): Expression {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitParentheses(this)
}
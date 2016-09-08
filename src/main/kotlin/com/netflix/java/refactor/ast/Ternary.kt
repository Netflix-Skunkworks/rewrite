package com.netflix.java.refactor.ast

data class Ternary(val condition: Expression,
                   val truePart: Expression,
                   val falsePart: Expression,
                   override val type: Type?,
                   override val pos: IntRange): Expression {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitTernary(this)
}
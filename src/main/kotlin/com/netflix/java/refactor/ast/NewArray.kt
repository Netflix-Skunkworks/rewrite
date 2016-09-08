package com.netflix.java.refactor.ast


data class NewArray(val typeExpr: Expression,
                    val dimensions: List<Expression>,
                    val elements: List<Expression>,
                    override val type: Type?,
                    override val pos: IntRange): Expression {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitNewArray(this)
}
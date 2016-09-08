package com.netflix.java.refactor.ast

data class Assign(val variable: Expression,
                  val assignment: Expression,
                  override val type: Type?,
                  override val pos: IntRange): Expression, Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitAssign(this)
}
package com.netflix.java.refactor.ast

data class Switch(val selector: Parentheses,
                  val cases: List<Case>,
                  override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitSwitch(this)
}
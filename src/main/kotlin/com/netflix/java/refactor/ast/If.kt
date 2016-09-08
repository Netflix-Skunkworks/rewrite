package com.netflix.java.refactor.ast

data class If(val ifCondition: Parentheses,
              val thenPart: Statement,
              val elsePart: Statement?,
              override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitIf(this)
    
}
package com.netflix.java.refactor.ast


data class ForLoop(val init: List<Statement>,
                   val condition: Expression?,
                   val update: List<Statement>,
                   val body: Statement,
                   override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitForLoop(this)
}
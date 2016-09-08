package com.netflix.java.refactor.ast

data class ForEachLoop(val variable: VariableDecl,
                       val iterable: Expression,
                       val body: Statement,
                       override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitForEachLoop(this)
}
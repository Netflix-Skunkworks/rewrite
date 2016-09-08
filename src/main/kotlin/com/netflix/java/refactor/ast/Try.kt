package com.netflix.java.refactor.ast

data class Try(val resources: List<VariableDecl>,
               val body: Block,
               val catchers: List<Catch>,
               val finally: Block?,
               override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitTry(this)
}
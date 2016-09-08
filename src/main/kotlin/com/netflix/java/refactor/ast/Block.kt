package com.netflix.java.refactor.ast

class Block(val statements: List<Statement>,
            override val pos: IntRange): Statement {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitBlock(this)
}
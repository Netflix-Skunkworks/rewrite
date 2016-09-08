package com.netflix.java.refactor.ast

class Label(val label: String,
            val statement: Statement,
            override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitLabel(this)
}
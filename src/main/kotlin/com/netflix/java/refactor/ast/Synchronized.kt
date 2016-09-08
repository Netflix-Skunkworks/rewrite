package com.netflix.java.refactor.ast

data class Synchronized(val lock: Parentheses,
                        val body: Block,
                        override val pos: IntRange): Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitSynchronized(this)
}

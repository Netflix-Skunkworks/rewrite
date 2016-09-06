package com.netflix.java.refactor.tree

class JRLabel(val label: String,
              val statement: JRStatement,
              override val pos: IntRange): JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitLabel(this)
}
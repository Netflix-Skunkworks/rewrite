package com.netflix.java.refactor.tree

class JRBlock(val statements: List<JRStatement>,
              override val pos: IntRange): JRStatement {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitBlock(this)
}
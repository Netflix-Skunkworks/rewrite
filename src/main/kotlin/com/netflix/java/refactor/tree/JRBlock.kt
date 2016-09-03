package com.netflix.java.refactor.tree

class JRBlock(val statements: List<JRTree>,
              override val pos: IntRange): JRTree {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitBlock(this)
}
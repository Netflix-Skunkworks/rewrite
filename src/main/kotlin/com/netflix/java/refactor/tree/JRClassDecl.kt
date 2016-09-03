package com.netflix.java.refactor.tree

class JRClassDecl(
        val name: String,
        val fields: List<JRVariableDecl>,
        val methods: List<JRMethodDecl>,
        val extends: JRTree?,
        val implements: List<JRTree>,
        override val pos: IntRange): JRTree {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitClassDecl(this)
}
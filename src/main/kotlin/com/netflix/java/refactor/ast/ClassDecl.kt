package com.netflix.java.refactor.ast

class ClassDecl(
        val name: String,
        val fields: List<VariableDecl>,
        val methods: List<MethodDecl>,
        val extends: Tree?,
        val implements: List<Tree>,
        val type: Type?,
        override val pos: IntRange): Tree {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitClassDecl(this)
}
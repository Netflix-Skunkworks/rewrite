package com.netflix.java.refactor.ast

class VariableDecl(
        val name: String,
        val nameExpr: Expression?,
        val varType: Expression?,
        val initializer: Expression?,
        val type: Type?,
        override val pos: IntRange): Statement {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitVariable(this)
}
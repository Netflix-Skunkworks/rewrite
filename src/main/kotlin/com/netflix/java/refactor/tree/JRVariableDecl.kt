package com.netflix.java.refactor.tree

class JRVariableDecl(
        val name: String,
        val nameExpr: JRExpression?,
        val varType: JRExpression?,
        val initializer: JRExpression?,
        val type: JRType?,
        override val pos: IntRange): JRStatement {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitVariable(this)
}
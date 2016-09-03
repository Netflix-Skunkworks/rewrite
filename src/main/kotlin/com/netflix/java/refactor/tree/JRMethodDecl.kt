package com.netflix.java.refactor.tree

class JRMethodDecl(val name: String,
                   val returnTypeExpr: JRExpression?,
                   val params: List<JRVariableDecl>,
                   val thrown: List<JRExpression>,
                   val body: JRBlock,
                   val defaultValue: JRExpression?,
                   override val pos: IntRange): JRTree {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitMethod(this)
}
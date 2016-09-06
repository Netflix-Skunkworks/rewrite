package com.netflix.java.refactor.tree

data class JRLambda(val params: List<JRVariableDecl>,
                    val body: JRTree,
                    override val pos: IntRange,
                    override val source: String): JRExpression {

    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitLambda(this)
}

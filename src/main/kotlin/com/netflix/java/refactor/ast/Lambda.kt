package com.netflix.java.refactor.ast

data class Lambda(val params: List<VariableDecl>,
                  val body: Tree,
                  override val type: Type?,
                  override val pos: IntRange): Expression {

    override fun <R> accept(v: AstVisitor<R>): R = v.visitLambda(this)
}

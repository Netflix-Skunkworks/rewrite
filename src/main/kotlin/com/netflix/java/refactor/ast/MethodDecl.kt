package com.netflix.java.refactor.ast

class MethodDecl(val name: String,
                 val returnTypeExpr: Expression?,
                 val params: List<VariableDecl>,
                 val thrown: List<Expression>,
                 val body: Block,
                 val defaultValue: Expression?,
                 override val pos: IntRange): Tree {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitMethod(this)
}
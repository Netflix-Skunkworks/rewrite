package com.netflix.java.refactor.tree

class JRNewClass(val encl: JRExpression?,
                 val typeargs: List<JRExpression>,
                 val identifier: JRExpression,
                 val args: List<JRExpression>,
                 val classBody: JRClassDecl?, // non-null for anonymous classes
                 val type: JRType?,
                 override val pos: IntRange,
                 override val source: String): JRExpression {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitNewClass(this)
}
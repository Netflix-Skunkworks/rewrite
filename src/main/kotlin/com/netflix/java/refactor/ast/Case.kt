package com.netflix.java.refactor.ast

data class Case(val pattern: Expression?, // null for the default case
                val statements: List<Statement>,
                override val pos: IntRange): Statement {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitCase(this)
}
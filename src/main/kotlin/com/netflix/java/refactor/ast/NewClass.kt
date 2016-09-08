package com.netflix.java.refactor.ast

class NewClass(val encl: Expression?,
               val typeargs: List<Expression>,
               val identifier: Expression,
               val args: List<Expression>,
               val classBody: ClassDecl?, // non-null for anonymous classes
               override val type: Type?,
               override val pos: IntRange): Expression, Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitNewClass(this)
}
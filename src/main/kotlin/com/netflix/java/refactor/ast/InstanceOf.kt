package com.netflix.java.refactor.ast

data class InstanceOf(val expr: Expression,
                      val clazz: Tree,
                      override val type: Type?,
                      override val pos: IntRange): Expression {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitInstanceOf(this) 
}
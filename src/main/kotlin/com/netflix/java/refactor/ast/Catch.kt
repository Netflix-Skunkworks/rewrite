package com.netflix.java.refactor.ast

data class Catch(val param: VariableDecl,
                 val body: Block,
                 override val pos: IntRange): Tree {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitCatch(this)
}
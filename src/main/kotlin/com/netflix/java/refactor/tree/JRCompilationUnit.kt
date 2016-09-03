package com.netflix.java.refactor.tree

data class JRCompilationUnit(val source: String,
                             val imports: List<JRImport>,
                             val classDecls: List<JRClassDecl>,
                             override val pos: IntRange): JRTree {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitCompilationUnit(this)
}

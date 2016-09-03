package com.netflix.java.refactor.tree

data class JRImport(val qualid: JRFieldAccess,
                    override val pos: IntRange): JRTree {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitImport(this)
    
    fun matches(clazz: String): Boolean = when(qualid.fieldName) {
        "*" -> qualid.target.source == clazz.split('.').takeWhile { it[0].isLowerCase() }.joinToString(".")
        else -> qualid.source == clazz
    }
}
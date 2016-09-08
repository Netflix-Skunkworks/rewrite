package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Source

data class Import(val qualid: FieldAccess,
                  val static: Boolean,
                  override val pos: IntRange): Tree {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitImport(this)
    
    fun matches(source: Source, clazz: String): Boolean = when(qualid.fieldName) {
        "*" -> source.snippet(qualid.target) == clazz.split('.').takeWhile { it[0].isLowerCase() }.joinToString(".")
        else -> source.snippet(qualid) == clazz
    }
}
package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.Import
import com.netflix.java.refactor.ast.AstVisitor

class HasImport(val clazz: String): AstVisitor<Boolean>(false) {
    override fun visitImport(import: Import): Boolean = import.matches(cu.source, clazz)
}
package com.netflix.java.refactor.find

import com.netflix.java.refactor.tree.JRImport
import com.netflix.java.refactor.tree.JRTreeVisitor

class HasImport2(val clazz: String): JRTreeVisitor<Boolean>(false) {
    override fun visitImport(import: JRImport): Boolean = import.matches(clazz)
}
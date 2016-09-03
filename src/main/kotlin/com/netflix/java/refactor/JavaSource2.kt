package com.netflix.java.refactor

import com.netflix.java.refactor.find.HasImport2
import com.netflix.java.refactor.find.HasType2
import com.netflix.java.refactor.tree.JRCompilationUnit

class JavaSource2(internal val cu: JRCompilationUnit) {
    fun text() = cu.source

    fun hasType(clazz: Class<*>): Boolean = HasType2(clazz.name).scan(cu)
    fun hasType(clazz: String): Boolean = HasType2(clazz).scan(cu)

    fun hasImport(clazz: Class<*>): Boolean = HasImport2(clazz.name).scan(cu)
    fun hasImport(clazz: String): Boolean = HasImport2(clazz).scan(cu)
}
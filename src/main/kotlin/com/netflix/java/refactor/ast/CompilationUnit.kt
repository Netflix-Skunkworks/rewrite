package com.netflix.java.refactor.ast

import com.netflix.java.refactor.diff.JavaSourceDiff
import com.netflix.java.refactor.parse.Source
import com.netflix.java.refactor.refactor.RefactorTransaction
import com.netflix.java.refactor.search.*

data class CompilationUnit(val source: Source,
                           val packageDecl: Expression?,
                           val imports: List<Import>,
                           val classDecls: List<ClassDecl>,
                           override val pos: IntRange): Tree {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitCompilationUnit(this)

    fun text() = source.text

    fun hasType(clazz: Class<*>): Boolean = HasType(clazz.name).visit(this)
    fun hasType(clazz: String): Boolean = HasType(clazz).visit(this)

    fun hasImport(clazz: Class<*>): Boolean = HasImport(clazz.name).visit(this)
    fun hasImport(clazz: String): Boolean = HasImport(clazz).visit(this)

    /**
     * Find fields defined on this class, but do not include inherited fields up the type hierarchy
     */
    fun findFields(clazz: Class<*>): List<Field> = FindFields(clazz.name, false).visit(this)
    fun findFields(clazz: String): List<Field> = FindFields(clazz, false).visit(this)

    /**
     * Find fields defined both on this class and visible inherited fields up the type hierarchy
     */
    fun findFieldsIncludingInherited(clazz: Class<*>): List<Field> = FindFields(clazz.name, true).visit(this)
    fun findFieldsIncludingInherited(clazz: String): List<Field> = FindFields(clazz, true).visit(this)

    fun findMethodCalls(signature: String): List<Method> = FindMethods(signature).visit(this)

    fun refactor() = RefactorTransaction(this)
    
    fun diff(body: CompilationUnit.() -> Unit): String {
        val diff = JavaSourceDiff(source)
        this.body()
        return diff.gitStylePatch()
    }

    fun beginDiff() = JavaSourceDiff(source)
}

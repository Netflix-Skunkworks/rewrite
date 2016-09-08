package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.refactor.op.*
import com.netflix.java.refactor.ast.CompilationUnit
import java.util.*

class RefactorTransaction(val cu: CompilationUnit) {
    private val ops = ArrayList<RefactorTreeVisitor>()

    fun changeType(from: String, to: String): RefactorTransaction {
        ops.add(ChangeType(cu.source, from, to))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>) = changeType(from.name, to.name)

    fun findMethodCalls(signature: String): ChangeMethodInvocation {
        val changeMethod = ChangeMethodInvocation(cu.source, signature, this)
        ops.add(changeMethod)
        return changeMethod
    }

    fun findFieldsOfType(clazz: Class<*>): ChangeField = findFieldsOfType(clazz.name)

    fun findFieldsOfType(clazz: String): ChangeField {
        val changeField = ChangeField(cu.source, clazz, this)
        ops.add(changeField)
        return changeField
    }

    fun removeImport(clazz: String): RefactorTransaction {
        ops.add(RemoveImport(cu.source, clazz))
        return this
    }

    fun removeImport(clazz: Class<*>) = removeImport(clazz.name)

    fun addImport(clazz: String): RefactorTransaction {
        ops.add(AddImport(cu.source, clazz))
        return this
    }

    fun addImport(clazz: Class<*>) = addImport(clazz.name)

    fun addStaticImport(clazz: String, method: String): RefactorTransaction {
        ops.add(AddImport(cu.source, clazz, method))
        return this
    }

    fun addStaticImport(clazz: Class<*>, method: String) = addStaticImport(clazz.name, method)
    
    fun addField(clazz: Class<*>, name: String, init: String?) = addField(clazz.name, name, init)
    
    fun addField(clazz: Class<*>, name: String) = addField(clazz.name, name, null)

    fun addField(clazz: String, name: String) = addField(clazz, name, null)
    
    fun addField(clazz: String, name: String, init: String?): RefactorTransaction {
        ops.add(AddField(cu.source, clazz, name, init))
        return this
    }
    
    fun fix() {
        val fixes = ops.flatMap { it.visit(cu) }

        if(fixes.isNotEmpty()) {
            try {
                val sourceText = cu.text()
                val sortedFixes = fixes.sortedBy { it.position.last }.sortedBy { it.position.start }
                var source = sortedFixes.foldIndexed("") { i, source, fix ->
                    val prefix = if (i == 0)
                        sourceText.substring(0, fix.position.first)
                    else sourceText.substring(sortedFixes[i - 1].position.last, fix.position.start)
                    source + prefix + (fix.changes ?: "")
                }
                if (sortedFixes.last().position.last < sourceText.length) {
                    source += sourceText.substring(sortedFixes.last().position.last)
                }
                
                cu.source.fix(source)
            } catch(t: Throwable) {
                // TODO how can we throw a better exception?
                t.printStackTrace()
            }
        }
    }
}
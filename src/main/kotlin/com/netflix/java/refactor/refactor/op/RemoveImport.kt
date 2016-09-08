package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.parse.Source
import com.netflix.java.refactor.refactor.RefactorFix
import com.netflix.java.refactor.refactor.RefactorTreeVisitor
import com.netflix.java.refactor.search.MethodMatcher
import java.util.*

class RemoveImport(override val source: Source, val clazz: String) : RefactorTreeVisitor() {
    val methodMatcher = MethodMatcher("$clazz *(..)")
    
    var namedImport: Import? = null
    var starImport: Import? = null

    var referencedTypes = ArrayList<Type.Class>()
    var referencedMethods = ArrayList<MethodInvocation>()

    var staticNamedImports = ArrayList<Import>()
    var staticStarImport: Import? = null

    override fun visitImport(import: Import): List<RefactorFix> {
        if (import.static) {
            if (source.snippet(import.qualid.target) == clazz) {
                if (import.qualid.fieldName == "*")
                    staticStarImport = import
                else
                    staticNamedImports.add(import)
            }
        } else {
            if (source.snippet(import.qualid) == clazz) {
                namedImport = import
            } else if (import.qualid.fieldName == "*" && clazz.startsWith(source.snippet(import.qualid.target))) {
                starImport = import
            }
        }

        return emptyList()
    }
    
    override fun visitIdentifier(ident: Ident): List<RefactorFix> {
        val pkg = ident.type.asClass()?.owner.asPackage()?.fullName
        if(pkg is String && clazz.startsWith(pkg))
            ident.type.asClass()?.let { referencedTypes.add(it) }
        return emptyList()
    }

    override fun visitMethodInvocation(meth: MethodInvocation): List<RefactorFix> {
        if(methodMatcher.matches(meth)) {
            if(meth.declaringType?.fullyQualifiedName == clazz)
               referencedMethods.add(meth)
        }
        return super.visitMethodInvocation(meth)
    }

    override fun visitEnd(): List<RefactorFix> =
        classImportDeletions() + staticImportDeletions()

    private fun classImportDeletions() = 
        if (namedImport is Import && referencedTypes.none { it.toString() == clazz }) {
            listOf(namedImport!!.delete())
        } else if (starImport is Import && referencedTypes.isEmpty()) {
            listOf(starImport!!.delete())
        } else if (starImport is Import && referencedTypes.size == 1) {
            listOf(starImport!!.replace("import ${referencedTypes[0].fullyQualifiedName};"))
        } else emptyList()
    
    private fun staticImportDeletions(): ArrayList<RefactorFix> {
        val staticImportFixes = ArrayList<RefactorFix>()
        if(staticStarImport is Import && referencedMethods.isEmpty()) {
            staticImportFixes.add(staticStarImport!!.delete())
        }
        staticNamedImports.forEach { staticImport ->
            val method = staticImport.qualid.fieldName
            if(referencedMethods.none { ref -> ref.methodName() == method })
                staticImportFixes.add(staticImport.delete())
        }
        return staticImportFixes
    }
}
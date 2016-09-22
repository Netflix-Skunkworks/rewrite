package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.refactor.fix.RefactorFix
import com.netflix.java.refactor.refactor.fix.RefactorTreeVisitor
import com.netflix.java.refactor.search.MethodMatcher
import java.util.*

class RemoveImport(val clazz: String) : RefactorTreeVisitor() {
    val methodMatcher = MethodMatcher("$clazz *(..)")
    
    var namedImport: Tr.Import? = null
    var starImport: Tr.Import? = null

    var referencedTypes = ArrayList<Type.Class>()
    var referencedMethods = ArrayList<Tr.MethodInvocation>()

    var staticNamedImports = ArrayList<Tr.Import>()
    var staticStarImport: Tr.Import? = null

    override fun visitImport(import: Tr.Import, cursor: Cursor): List<RefactorFix> {
        if (import.static) {
            if (import.qualid.target.source.text(cu) == clazz) {
                if (import.qualid.fieldName == "*")
                    staticStarImport = import
                else
                    staticNamedImports.add(import)
            }
        } else {
            if (import.qualid.source.text(cu) == clazz) {
                namedImport = import
            } else if (import.qualid.fieldName == "*" && clazz.startsWith(import.qualid.target.source.text(cu))) {
                starImport = import
            }
        }

        return emptyList()
    }
    
    override fun visitIdentifier(ident: Tr.Ident, cursor: Cursor): List<RefactorFix> {
        val pkg = ident.type.asClass()?.owner.asPackage()?.fullName
        if(pkg is String && clazz.startsWith(pkg))
            ident.type.asClass()?.let { referencedTypes.add(it) }
        return emptyList()
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation, cursor: Cursor): List<RefactorFix> {
        if(methodMatcher.matches(meth)) {
            if(meth.declaringType?.fullyQualifiedName == clazz)
               referencedMethods.add(meth)
        }
        return super.visitMethodInvocation(meth, cursor)
    }

    override fun visitEnd(): List<RefactorFix> =
        classImportDeletions() + staticImportDeletions()

    private fun classImportDeletions() = 
        if (namedImport is Tr.Import && referencedTypes.none { it.toString() == clazz }) {
            listOf(namedImport!!.delete())
        } else if (starImport is Tr.Import && referencedTypes.isEmpty()) {
            listOf(starImport!!.delete())
        } else if (starImport is Tr.Import && referencedTypes.size == 1) {
            listOf(starImport!!.replace("import ${referencedTypes[0].fullyQualifiedName};"))
        } else emptyList()
    
    private fun staticImportDeletions(): ArrayList<RefactorFix> {
        val staticImportFixes = ArrayList<RefactorFix>()
        if(staticStarImport is Tr.Import && referencedMethods.isEmpty()) {
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
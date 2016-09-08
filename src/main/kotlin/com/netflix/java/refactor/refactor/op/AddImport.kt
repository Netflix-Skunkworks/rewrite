package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Expression
import com.netflix.java.refactor.ast.Import
import com.netflix.java.refactor.parse.Source
import com.netflix.java.refactor.refactor.RefactorFix
import com.netflix.java.refactor.refactor.RefactorTreeVisitor
import java.util.*

class AddImport(override val source: Source, val clazz: String, val staticMethod: String? = null): RefactorTreeVisitor() {
    val imports = ArrayList<Import>()
    var coveredByExistingImport = false
    
    private val packageComparator = PackageComparator()

    override fun visitImport(import: Import): List<RefactorFix> {
        imports.add(import)
        val importedType = import.qualid.fieldName
        
        if (addingStaticImport()) {
            if (import.matches(source, clazz) && importedType == staticMethod) {
                coveredByExistingImport = true
            }
            if (import.matches(source, clazz) && importedType == "*") {
                coveredByExistingImport = true
            }
        }
        else {
            if (import.matches(source, clazz)) {
                coveredByExistingImport = true
            } else if (source.snippet(import.qualid.target) == packageOwner(clazz) && importedType == "*") {
                coveredByExistingImport = true
            }
        }
        
        return emptyList()
    }

    override fun visitEnd(): List<RefactorFix> {
        val lastPrior = lastPriorImport()
        val importStatementToAdd = if(addingStaticImport()) {
            "import static $clazz.$staticMethod;"
        } else "import $clazz;"
        
        return if(coveredByExistingImport) {
            emptyList()
        }
        else if(lastPrior == null && imports.isNotEmpty()) {
            listOf(imports.first().insertBefore("$importStatementToAdd\n"))
        }
        else if(lastPrior is Import) {
            listOf(lastPrior.insertAt("$importStatementToAdd\n"))
        }
        else if(cu.packageDecl is Expression) {
            listOf(cu.packageDecl!!.insertAt("\n\n$importStatementToAdd"))
        }
        else listOf(cu.insertBefore("$importStatementToAdd\n"))
    }
    
    fun lastPriorImport(): Import? {
        return imports.lastOrNull { import ->
            // static imports go after all non-static imports
            if(addingStaticImport() && !import.static)
                return@lastOrNull true
            
            // non-static imports should always go before static imports
            if(!addingStaticImport() && import.static)
                return@lastOrNull false
            
            val comp = packageComparator.compare(source.snippet(import.qualid.target), 
                    if(addingStaticImport()) clazz else packageOwner(clazz))
            if(comp == 0) {
                if(import.qualid.fieldName.toString().compareTo(
                        if(addingStaticImport()) staticMethod!! else className(clazz)) < 0) 
                    true 
                else false
            }
            else if(comp < 0) true
            else false
        }
    }
    
    fun addingStaticImport() = staticMethod is String
}
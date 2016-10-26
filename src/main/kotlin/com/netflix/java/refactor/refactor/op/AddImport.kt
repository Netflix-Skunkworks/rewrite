package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.fix.RefactorFix
import com.netflix.java.refactor.refactor.fix.RefactorTreeVisitor
import java.util.*

class AddImport(val clazz: String, val staticMethod: String? = null): RefactorTreeVisitor() {
    val imports = ArrayList<Tr.Import>()
    var coveredByExistingImport = false
    
    private val packageComparator = PackageComparator()

    override fun visitImport(import: Tr.Import): List<RefactorFix> {
        imports.add(import)
        val importedType = import.qualid.name.name
        
        if (addingStaticImport()) {
            if (import.matches(clazz) && importedType == staticMethod) {
                coveredByExistingImport = true
            }
            if (import.matches(clazz) && importedType == "*") {
                coveredByExistingImport = true
            }
        }
        else {
            if (import.matches(clazz)) {
                coveredByExistingImport = true
            } else if (import.qualid.target.print() == packageOwner(clazz) && importedType == "*") {
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
        else if(lastPrior is Tr.Import) {
            listOf(lastPrior.insertAt("$importStatementToAdd\n"))
        }
        else if(cu.packageDecl is Tr.Package) {
            listOf(cu.packageDecl!!.insertAt("\n\n$importStatementToAdd"))
        }
        else listOf(cu.insertBefore("$importStatementToAdd\n"))
    }
    
    fun lastPriorImport(): Tr.Import? {
        return imports.lastOrNull { import ->
            // static imports go after all non-static imports
            if(addingStaticImport() && !import.static)
                return@lastOrNull true
            
            // non-static imports should always go before static imports
            if(!addingStaticImport() && import.static)
                return@lastOrNull false
            
            val comp = packageComparator.compare(import.qualid.target.print(), 
                    if(addingStaticImport()) clazz else packageOwner(clazz))
            if(comp == 0) {
                if(import.qualid.name.toString().compareTo(
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
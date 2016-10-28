package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.asClass
import com.netflix.java.refactor.refactor.RefactorTransaction
import com.netflix.java.refactor.refactor.fix.RefactorFix
import com.netflix.java.refactor.refactor.fix.RefactorTreeVisitor
import java.util.*

data class ChangeField(val clazz: String, val tx: RefactorTransaction) : RefactorTreeVisitor() {
    var refactorTargetType: String? = null
    var refactorName: String? = null
    var refactorDelete: Boolean = false
    
    fun changeType(clazz: String): ChangeField {
        refactorTargetType = clazz
        return this
    }
    
    fun changeType(clazz: Class<*>) = changeType(clazz.name)

    fun changeName(name: String): ChangeField {
        refactorName = name
        return this
    }
    
    fun delete(): RefactorTransaction {
        refactorDelete = true
        return done()
    }
    
    fun done() = tx

//    override fun scanner() =
//            if (refactorTargetType != null) {
//                IfThenScanner(
//                        ifFixesResultFrom = ChangeFieldScanner(this),
//                        then = arrayOf(
//                                AddImport(refactorTargetType!!).scanner(),
//                                RemoveImport(clazz).scanner()
//                        )
//                )
//            } else ChangeFieldScanner(this)

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<RefactorFix> =
        multiVariable.vars.fold(emptyList()) { fixes, variable ->
            if (variable.type.asClass()?.fullyQualifiedName == clazz) {
                fixes + refactorField(multiVariable, variable)
            } else fixes
        }

    private fun refactorField(decls: Tr.VariableDecls, variable: Tr.VariableDecls.NamedVar): ArrayList<RefactorFix> {
        val fixes = ArrayList<RefactorFix>()

        if (refactorDelete) {
            fixes.add(decls.delete())
            return fixes
        }
        
        if (refactorTargetType is String && !decls.typeExpr.matches(refactorTargetType)) {
            fixes.add(decls.typeExpr.replace(className(refactorTargetType!!)))
        }

        if (refactorName is String && variable.name.toString() != refactorName) {
            // unfortunately name is not represented with a JCTree, so we have to resort to extraordinary measures...
            val original = variable.name.toString()
            
//            val start = (decls.formatting as Formatting.Persisted).pos.start + decls.printTrimmed().substringBefore(original).length
            
//            fixes.add(replace(start..start+original.length, refactorName!!))
        }

        return fixes
    }
}
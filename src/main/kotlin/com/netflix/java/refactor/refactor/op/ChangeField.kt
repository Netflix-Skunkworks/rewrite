package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Source
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.asClass
import com.netflix.java.refactor.refactor.fix.RefactorFix
import com.netflix.java.refactor.refactor.RefactorTransaction
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

    override fun visitVariable(variable: Tr.VariableDecl, cursor: Cursor): List<RefactorFix> {
        if(variable.type.asClass()?.fullyQualifiedName == clazz) {
            return refactorField(variable)
        }
        return super.visitVariable(variable, cursor)
    }

    private fun refactorField(decl: Tr.VariableDecl): ArrayList<RefactorFix> {
        val fixes = ArrayList<RefactorFix>()

        if (refactorDelete) {
            fixes.add(decl.delete())
            return fixes
        }
        
        if (decl.varType != null && refactorTargetType is String && !decl.varType.matches(refactorTargetType)) {
            fixes.add(decl.varType.replace(className(refactorTargetType!!)))
        }

        if (refactorName is String && decl.name.toString() != refactorName) {
            // unfortunately name is not represented with a JCTree, so we have to resort to extraordinary measures...
            val original = decl.name.toString()
            
            val start = (decl.source as Source.Persisted).pos.start + decl.source.text(cu).substringBefore(original).length
            
            fixes.add(replace(start..start+original.length, refactorName!!))
        }

        return fixes
    }
}
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.fix.RefactorFix
import com.netflix.java.refactor.refactor.fix.RefactorTreeVisitor

class AddField(val clazz: String, val name: String, val init: String?): RefactorTreeVisitor() {
    
    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<RefactorFix> {
        val assignment = if(init is String) " = $init" else ""
//        return listOf(insertAt(cu.source.text.indexOf('{', (classDecl.formatting as Formatting.Persisted).pos.start) + 1, 
//                "\n   ${className(clazz)} $name$assignment;"))
        return emptyList()
    }
}
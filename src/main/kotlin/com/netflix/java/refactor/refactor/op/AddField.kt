package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Source
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.fix.RefactorFix
import com.netflix.java.refactor.refactor.fix.RefactorTreeVisitor

class AddField(val clazz: String, val name: String, val init: String?): RefactorTreeVisitor() {
    
    override fun visitClassDecl(classDecl: Tr.ClassDecl, cursor: Cursor): List<RefactorFix> {
        val assignment = if(init is String) " = $init" else ""
        return listOf(insertAt(cu.rawSource.text.indexOf('{', (classDecl.source as Source.Persisted).pos.start) + 1, 
                "\n   ${className(clazz)} $name$assignment;"))
    }
}
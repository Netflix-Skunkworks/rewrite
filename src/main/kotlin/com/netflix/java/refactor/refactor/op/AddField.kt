package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.ClassDecl
import com.netflix.java.refactor.parse.Source
import com.netflix.java.refactor.refactor.RefactorFix
import com.netflix.java.refactor.refactor.RefactorTreeVisitor

class AddField(override val source: Source, val clazz: String, val name: String, val init: String?): RefactorTreeVisitor() {
    override fun visitClassDecl(classDecl: ClassDecl): List<RefactorFix> {
        val assignment = if(init is String) " = $init" else ""
        return listOf(insertAt(source.text.indexOf('{', classDecl.pos.start) + 1, 
                "\n   ${className(clazz)} $name$assignment;"))
    }
}
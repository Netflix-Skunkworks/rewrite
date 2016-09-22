package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.asClass
import com.netflix.java.refactor.refactor.fix.RefactorFix
import com.netflix.java.refactor.refactor.fix.RefactorTreeVisitor

data class ChangeType(val from: String, val to: String): RefactorTreeVisitor() {
//    override fun scanner() = IfThenScanner(
//            ifFixesResultFrom = ChangeTypeScanner(this),
//            then = arrayOf(
//                RemoveImport(from).scanner(),
//                AddImport(to).scanner()
//            )
//    )

    override fun visitIdentifier(ident: Tr.Ident, cursor: Cursor): List<RefactorFix> =
        if(ident.type.asClass()?.fullyQualifiedName == from)
            listOf(ident.replace(className(to)))
        else emptyList()
}
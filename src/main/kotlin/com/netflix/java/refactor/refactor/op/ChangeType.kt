package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.Ident
import com.netflix.java.refactor.ast.asClass
import com.netflix.java.refactor.parse.Source
import com.netflix.java.refactor.refactor.RefactorFix
import com.netflix.java.refactor.refactor.RefactorTreeVisitor

data class ChangeType(override val source: Source, 
                      val from: String, val to: String): RefactorTreeVisitor() {
//    override fun scanner() = IfThenScanner(
//            ifFixesResultFrom = ChangeTypeScanner(this),
//            then = arrayOf(
//                RemoveImport(from).scanner(),
//                AddImport(to).scanner()
//            )
//    )

    override fun visitIdentifier(ident: Ident): List<RefactorFix> =
        if(ident.type.asClass()?.fullyQualifiedName == from)
            listOf(ident.replace(className(to)))
        else emptyList()
}
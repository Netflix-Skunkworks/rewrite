package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

class ChangeMethodName(val cu: Tr.CompilationUnit, val meth: Tr.MethodInvocation, val name: String) : RefactorVisitor() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if (meth === this.meth) {
            if(meth.name.name != name) {
                return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                    it.copy(name = it.name.copy(name = name))
                })
            }
        }
        return emptyList()
    }
}
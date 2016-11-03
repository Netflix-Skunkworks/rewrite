package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

data class ChangeFieldName(val cu: Tr.CompilationUnit, val decls: Tr.VariableDecls, val name: String) : RefactorVisitor() {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<*>> {
        if(multiVariable === decls) {
            assert(multiVariable.vars.size == 1) { "Refactor name is not supported on multi-variable declarations" }

            val v = multiVariable.vars.first()
            if(v.name.name != name) {
                return listOf(AstTransform<Tr.VariableDecls>(cursor()) {
                    decls.copy(vars = listOf(v.copy(name = Tr.Ident(name, v.name.type, v.name.formatting))))
                })
            }
        }
        return emptyList()
    }
}
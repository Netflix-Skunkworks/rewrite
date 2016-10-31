package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.*

class FindFields(val fullyQualifiedName: String) : AstVisitor<List<Tr.VariableDecls>>(emptyList()) {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<Tr.VariableDecls> {
        return if(multiVariable.typeExpr.type.hasElementType(fullyQualifiedName))
            listOf(multiVariable)
        else emptyList()
    }
}
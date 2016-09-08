package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.AstVisitor
import com.netflix.java.refactor.ast.MethodInvocation

data class Method(val name: String, val source: String)

class FindMethods(signature: String): AstVisitor<List<Method>>(emptyList()) {
    val matcher = MethodMatcher(signature)
    
    override fun visitMethodInvocation(meth: MethodInvocation): List<Method> {
        if(matcher.matches(meth)) {
            return listOf(Method(meth.toString(), meth.source()))
        }
        return super.visitMethodInvocation(meth)
    }
}
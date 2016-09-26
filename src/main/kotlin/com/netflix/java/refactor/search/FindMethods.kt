package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.AstVisitor
import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tr

data class Method(val name: String, val source: String)

class FindMethods(signature: String): AstVisitor<List<Method>>(emptyList()) {
    val matcher = MethodMatcher(signature)
    
    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<Method> {
        if(matcher.matches(meth)) {
            return listOf(Method(meth.toString(), meth.print()))
        }
        return super.visitMethodInvocation(meth)
    }
}
package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.*

class HasType(val clazz: String): AstVisitor<Boolean>(false) {
    override fun visitIdentifier(ident: Ident): Boolean =
        ident.type is Type.Class && ident.type.fullyQualifiedName == clazz

    override fun visitMethodInvocation(meth: MethodInvocation): Boolean {
        if(meth.methodSelect is Ident) {
            // statically imported type
            return meth.declaringType?.fullyQualifiedName == clazz
        }
        return super.visitMethodInvocation(meth)
    }
}
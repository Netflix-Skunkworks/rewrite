package com.netflix.java.refactor.find

import com.netflix.java.refactor.tree.JRIdent
import com.netflix.java.refactor.tree.JRMethodInvocation
import com.netflix.java.refactor.tree.JRTreeVisitor
import com.netflix.java.refactor.tree.JRType

class HasType2(val clazz: String): JRTreeVisitor<Boolean>(false) {
    override fun visitIdentifier(ident: JRIdent): Boolean =
        ident.type is JRType.Class && ident.type.fullyQualifiedName == clazz

    override fun visitMethodInvocation(meth: JRMethodInvocation): Boolean {
        if(meth.methodSelect is JRIdent) {
            // statically imported type
            return meth.declaringType?.fullyQualifiedName == clazz
        }
        return super.visitMethodInvocation(meth)
    }
}
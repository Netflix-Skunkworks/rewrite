package com.netflix.java.refactor.tree

class JRMethodInvocation(val methodSelect: JRExpression,
                         val args: List<JRExpression>,
                         val genericSignature: JRType.Method?,
     
                         // in the case of generic signature parts, this concretizes 
                         // them relative to the call site
                         val resolvedSignature: JRType.Method?,

                         val declaringType: JRType.Class?,
                         override val pos: IntRange,
                         override val source: String): JRExpression {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitMethodInvocation(this)
    
    fun returnType(): JRType? = resolvedSignature?.returnType
}
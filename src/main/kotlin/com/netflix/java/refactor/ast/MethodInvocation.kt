package com.netflix.java.refactor.ast

class MethodInvocation(val methodSelect: Expression,
                       val args: List<Expression>,
                       val genericSignature: Type.Method?,
     
                         // in the case of generic signature parts, this concretizes 
                         // them relative to the call site
                       val resolvedSignature: Type.Method?,

                       val declaringType: Type.Class?,
                       
                       override val pos: IntRange): Expression, Statement {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitMethodInvocation(this)
    
    override val type = resolvedSignature?.returnType
    
    fun returnType(): Type? = resolvedSignature?.returnType
    
    fun methodName(): String = when(methodSelect) {
        is FieldAccess -> methodSelect.fieldName
        is Ident -> methodSelect.name
        else -> throw IllegalStateException("Unexpected method select type ${methodSelect}")
    }
}
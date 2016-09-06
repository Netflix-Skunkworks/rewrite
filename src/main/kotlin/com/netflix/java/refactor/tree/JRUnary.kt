package com.netflix.java.refactor.tree

data class JRUnary(val operator: Operator,
                   val expr: JRExpression,
                   val type: JRType?, 
                   override val pos: IntRange,
                   override val source: String): JRExpression {
    
    enum class Operator {
        // Arithmetic
        PreIncrement, PreDecrement, PostIncrement, PostDecrement, Positive, Negative,
        
        // Bitwise
        Complement,
        
        // Boolean
        Not
    }

    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitUnary(this)
}

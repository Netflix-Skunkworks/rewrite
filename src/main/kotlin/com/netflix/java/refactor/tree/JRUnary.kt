package com.netflix.java.refactor.tree

/**
 * Increment and decrement operations are valid statements, other operations are not
 */
data class JRUnary(val operator: Operator,
                   val expr: JRExpression,
                   val type: JRType?, 
                   override val pos: IntRange,
                   override val source: String): JRExpression, JRStatement {
    
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

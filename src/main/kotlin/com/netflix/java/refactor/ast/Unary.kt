package com.netflix.java.refactor.ast

/**
 * Increment and decrement operations are valid statements, other operations are not
 */
data class Unary(val operator: Operator,
                 val expr: Expression,
                 override val type: Type?,
                 override val pos: IntRange): Expression, Statement {
    
    enum class Operator {
        // Arithmetic
        PreIncrement, PreDecrement, PostIncrement, PostDecrement, Positive, Negative,
        
        // Bitwise
        Complement,
        
        // Boolean
        Not
    }

    override fun <R> accept(v: AstVisitor<R>): R = v.visitUnary(this)
}

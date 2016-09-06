package com.netflix.java.refactor.tree

data class JRBinary(val operator: Operator,
                    val left: JRExpression,
                    val right: JRExpression,
                    val type: JRType?, 
                    override val pos: IntRange,
                    override val source: String): JRExpression {
    
    enum class Operator {
        // Arithmetic
        Addition, Subtraction, Multiplication, Division, Modulo,
        
        // Relational
        LessThan, GreaterThan, LessThanOrEqual, GreaterThanOrEqual, Equal, NotEqual,
        
        // Bitwise
        BitAnd, BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift,
        
        // Boolean
        Or, And
    }

    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitBinary(this)
}

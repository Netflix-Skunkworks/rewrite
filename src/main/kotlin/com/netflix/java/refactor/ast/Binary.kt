package com.netflix.java.refactor.ast

data class Binary(val operator: Operator,
                  val left: Expression,
                  val right: Expression,
                  override val type: Type?,
                  override val pos: IntRange): Expression {
    
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

    override fun <R> accept(v: AstVisitor<R>): R = v.visitBinary(this)
}

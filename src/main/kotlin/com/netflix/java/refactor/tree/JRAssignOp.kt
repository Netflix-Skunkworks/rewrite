package com.netflix.java.refactor.tree

class JRAssignOp(val operator: Operator,
                 val variable: JRExpression,
                 val assignment: JRExpression,
                 override val pos: IntRange,
                 override val source: String): JRExpression, JRStatement {
    
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitAssignOp(this)

    enum class Operator {
        // Arithmetic
        Addition, Subtraction, Multiplication, Division, Modulo,
        
        // Bitwise
        BitAnd, BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift
    }
}
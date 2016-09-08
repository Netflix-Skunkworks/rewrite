package com.netflix.java.refactor.ast

class AssignOp(val operator: Operator,
               val variable: Expression,
               val assignment: Expression,
               override val type: Type?,
               override val pos: IntRange): Expression, Statement {
    
    override fun <R> accept(v: AstVisitor<R>): R = v.visitAssignOp(this)

    enum class Operator {
        // Arithmetic
        Addition, Subtraction, Multiplication, Division, Modulo,
        
        // Bitwise
        BitAnd, BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift
    }
}
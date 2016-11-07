package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Expression
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

class ChangeLiteralArgument(val expr: Expression, val transform: (Any?) -> Any?): RefactorVisitor() {

    override fun visitExpression(expr: Expression): List<AstTransform<*>> {
        if(expr.id == this.expr.id) {
            return LiteralVisitor().visit(expr)
        }
        return super.visitExpression(expr)
    }

    private inner class LiteralVisitor(): RefactorVisitor() {
        override fun visitLiteral(literal: Tr.Literal): List<AstTransform<*>> {
            val transformed = transform.invoke(literal.value)
            return if(transformed != literal.value) {
                listOf(AstTransform<Tr.Literal>(this@ChangeLiteralArgument.cursor().parent() + cursor()) {
                    copy(value = transformed)
                })
            } else emptyList()
        }
    }
}
package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class JRUnaryTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun negation() {
        val a = parse("""
            public class A {
                boolean b = !(1 == 2);
            }
        """)

        val unary = a.classDecls[0].fields[0].initializer as JRUnary
        assertEquals(JRUnary.Operator.Not, unary.operator)
        assertTrue(unary.expr is JRParentheses)
    }
}
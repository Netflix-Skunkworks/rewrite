package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class UnaryTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun negation() {
        val a = parse("""
            public class A {
                boolean b = !(1 == 2);
            }
        """)

        val unary = a.classDecls[0].fields()[0].initializer as Tr.Unary
        assertTrue(unary.operator is Tr.Unary.Operator.Not)
        assertTrue(unary.expr is Tr.Parentheses)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int i = 0;
                int j = ++i;
            }
        """)

        val unary = a.classDecls[0].fields()[1].initializer as Tr.Unary
        assertEquals("++i", unary.print())
    }
}
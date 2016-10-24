package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

abstract class BreakTest(parser: Parser): AstTest(parser) {

    @Test
    fun breakFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) break;
                }
            }
        """)

        val whileLoop = a.firstMethodStatement() as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Break)
        assertNull((whileLoop.body as Tr.Break).label)
    }

    @Test
    fun breakFromLabeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Break)
        assertEquals("labeled", (whileLoop.body as Tr.Break).label?.name)
    }

    @Test
    fun formatLabeledBreak() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled : while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertEquals("break labeled", whileLoop.body.print())
    }
}
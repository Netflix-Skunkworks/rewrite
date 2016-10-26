package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

abstract class ContinueTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun continueFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) continue;
                }
            }
        """)
        
        val whileLoop = a.firstMethodStatement() as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Continue)
        assertNull((whileLoop.body as Tr.Continue).label)
    }

    @Test
    fun continueFromLabeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true)
                        continue labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Continue)
        assertEquals("labeled", (whileLoop.body as Tr.Continue).label?.name)
    }

    @Test
    fun formatContinueLabeled() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled : while(true)
                        continue labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertEquals("continue labeled", whileLoop.body.printTrimmed())
    }
}
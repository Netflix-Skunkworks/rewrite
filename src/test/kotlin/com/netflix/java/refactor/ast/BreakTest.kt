package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        
        val whileLoop = a.classDecls[0].methods()[0].body.statements[0] as Tr.WhileLoop
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

        val whileLoop = (a.classDecls[0].methods()[0].body.statements[0] as Tr.Label).statement as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Break)
        assertEquals("labeled", (whileLoop.body as Tr.Break).label)
    }
}
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
        
        val whileLoop = a.classDecls[0].methods[0].body.statements[0] as WhileLoop
        assertTrue(whileLoop.body is Break)
        assertNull((whileLoop.body as Break).label)
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

        val whileLoop = (a.classDecls[0].methods[0].body.statements[0] as Label).statement as WhileLoop
        assertTrue(whileLoop.body is Break)
        assertEquals("labeled", (whileLoop.body as Break).label)
    }
}
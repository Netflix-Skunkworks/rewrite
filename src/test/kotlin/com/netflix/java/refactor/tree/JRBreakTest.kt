package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class JRBreakTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun breakFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) break;
                }
            }
        """)
        
        val whileLoop = a.classDecls[0].methods[0].body.statements[0] as JRWhileLoop
        assertTrue(whileLoop.body is JRBreak)
        assertNull((whileLoop.body as JRBreak).label)
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

        val whileLoop = (a.classDecls[0].methods[0].body.statements[0] as JRLabel).statement as JRWhileLoop
        assertTrue(whileLoop.body is JRBreak)
        assertEquals("labeled", (whileLoop.body as JRBreak).label)
    }
}
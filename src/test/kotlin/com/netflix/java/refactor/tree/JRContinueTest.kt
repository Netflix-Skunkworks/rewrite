package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class JRContinueTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun continueFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) continue;
                }
            }
        """)
        
        val whileLoop = a.classDecls[0].methods[0].body.statements[0] as JRWhileLoop
        assertTrue(whileLoop.body is JRContinue)
        assertNull((whileLoop.body as JRContinue).label)
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

        val whileLoop = (a.classDecls[0].methods[0].body.statements[0] as JRLabel).statement as JRWhileLoop
        assertTrue(whileLoop.body is JRContinue)
        assertEquals("labeled", (whileLoop.body as JRContinue).label)
    }
}
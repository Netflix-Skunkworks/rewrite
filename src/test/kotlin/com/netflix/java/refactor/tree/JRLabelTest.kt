package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class JRLabelTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun labeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true) {
                    }
                }
            }
        """)
        
        val labeled = a.classDecls[0].methods[0].body.statements[0] as JRLabel
        assertEquals("labeled", labeled.label)
        assertTrue(labeled.statement is JRWhileLoop)
    }
}
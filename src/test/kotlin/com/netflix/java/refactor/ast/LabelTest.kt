package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

abstract class LabelTest(parser: Parser): AstTest(parser) {
    
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
        
        val labeled = a.firstMethodStatement() as Tr.Label
        assertEquals("labeled", labeled.label.name)
        assertTrue(labeled.statement is Tr.WhileLoop)
    }
}
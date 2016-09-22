package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        
        val labeled = a.classDecls[0].methods[0].body.statements[0] as Tr.Label
        assertEquals("labeled", labeled.label)
        assertTrue(labeled.statement is Tr.WhileLoop)
    }
}
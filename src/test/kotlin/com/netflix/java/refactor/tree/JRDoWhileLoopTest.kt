package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRDoWhileLoopTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun doWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    do {
                    } while(true);
                }
            }
        """)
        
        val whileLoop = a.classDecls[0].methods[0].body.statements[0] as JRDoWhileLoop
        assertTrue(whileLoop.condition is JRLiteral)
        assertTrue(whileLoop.body is JRBlock)
    }
}
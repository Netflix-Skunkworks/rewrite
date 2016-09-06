package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRWhileLoopTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun whileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) {
                    }
                }
            }
        """)
        
        val whileLoop = a.classDecls[0].methods[0].body.statements[0] as JRWhileLoop
        assertTrue(whileLoop.condition is JRLiteral)
        assertTrue(whileLoop.body is JRBlock)
    }
}
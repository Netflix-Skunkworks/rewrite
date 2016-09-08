package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class DoWhileLoopTest(parser: Parser): AstTest(parser) {
    
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
        
        val whileLoop = a.classDecls[0].methods[0].body.statements[0] as DoWhileLoop
        assertTrue(whileLoop.condition.expr is Literal)
        assertTrue(whileLoop.body is Block)
    }
}
package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRThrowTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun throwException() {
        val a = parse("""
            public class A {
                public void test() throws Exception {
                    throw new UnsupportedOperationException();
                }
            }
        """)
        
        val thrown = a.classDecls[0].methods[0].body.statements[0] as JRThrow
        assertTrue(thrown.expr is JRNewClass)
    }
}
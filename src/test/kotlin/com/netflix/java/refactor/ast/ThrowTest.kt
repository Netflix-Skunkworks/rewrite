package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class ThrowTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun throwException() {
        val a = parse("""
            public class A {
                public void test() throws Exception {
                    throw new UnsupportedOperationException();
                }
            }
        """)
        
        val thrown = a.classDecls[0].methods()[0].body.statements[0] as Tr.Throw
        assertTrue(thrown.expr is Tr.NewClass)
    }
}
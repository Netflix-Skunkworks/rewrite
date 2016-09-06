package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JREmptyTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun empty() {
        val a = parse("""
            public class A {
                public void test() {
                    ;
                }
            }
        """)
        
        assertTrue(a.classDecls[0].methods[0].body.statements[0] is JREmpty)
    }
}
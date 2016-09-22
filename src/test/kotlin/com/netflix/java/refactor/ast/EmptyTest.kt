package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class EmptyTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun empty() {
        val a = parse("""
            public class A {
                public void test() {
                    ;
                }
            }
        """)
        
        assertTrue(a.classDecls[0].methods[0].body.statements[0] is Tr.Empty)
    }
}
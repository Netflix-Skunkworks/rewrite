package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class ReturnTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun returnValue() {
        val a = parse("""
            public class A {
                public String test() {
                    return "";
                }
            }
        """)
        
        val rtn = a.classDecls[0].methods[0].body.statements[0] as Return
        assertTrue(rtn.expr is Literal)
    }

    @Test
    fun returnVoid() {
        val a = parse("""
            public class A {
                public void test() {
                    return;
                }
            }
        """)

        val rtn = a.classDecls[0].methods[0].body.statements[0] as Return
        assertNull(rtn.expr)
    }
}
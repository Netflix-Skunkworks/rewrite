package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class JRReturnTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun returnValue() {
        val a = parse("""
            public class A {
                public String test() {
                    return "";
                }
            }
        """)
        
        val rtn = a.classDecls[0].methods[0].body.statements[0] as JRReturn
        assertTrue(rtn.expr is JRLiteral)
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

        val rtn = a.classDecls[0].methods[0].body.statements[0] as JRReturn
        assertNull(rtn.expr)
    }
}
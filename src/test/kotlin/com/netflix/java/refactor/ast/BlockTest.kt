package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class BlockTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun methodBlock() {
        val a = parse("""
            public class A {
                public void foo() {
                    System.out.println("foo");
                }
            }
        """)
        
        assertEquals(1, a.typeDecls[0].methods()[0].body!!.statements.size)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public void foo() {  }
            }
        """)
        
        assertEquals("{  }", a.typeDecls[0].methods()[0].body!!.printTrimmed())
    }
}
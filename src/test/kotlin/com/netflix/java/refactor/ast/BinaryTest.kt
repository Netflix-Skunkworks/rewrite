package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class BinaryTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun arithmetic() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)
        
        val bin = a.classDecls[0].fields()[0].initializer as Tr.Binary
        assertTrue(bin.operator is Tr.Binary.Operator.Addition)
        assertTrue(bin.left is Tr.Literal)
        assertTrue(bin.right is Tr.Literal)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)

        val bin = a.classDecls[0].fields()[0].initializer as Tr.Binary
        assertEquals("0 + 1", bin.print())
    }
}

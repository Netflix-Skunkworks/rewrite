package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class BinaryTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun arithmetic() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)
        
        val bin = a.classDecls[0].fields[0].initializer as Tr.Binary
        assertEquals(Tr.Binary.Operator.Addition, bin.operator)
        assertTrue(bin.left is Tr.Literal)
        assertTrue(bin.right is Tr.Literal)
    }
}

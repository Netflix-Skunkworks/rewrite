package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class JRBinaryTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun arithmetic() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)
        
        val bin = a.classDecls[0].fields[0].initializer as JRBinary
        assertEquals(JRBinary.Operator.Addition, bin.operator)
        assertTrue(bin.left is JRLiteral)
        assertTrue(bin.right is JRLiteral)
    }
}

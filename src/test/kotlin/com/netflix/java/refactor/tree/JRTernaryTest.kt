package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class JRTernaryTest(parser: Parser): AstTest(parser) {
    @Test
    fun ternary() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    String evenOrOdd = n % 2 == 0 ? "even" : "odd";
                }
            }
        """)

        val evenOrOdd = a.classDecls[0].methods[0].body.statements[0] as JRVariableDecl
        val ternary = evenOrOdd.initializer as JRTernary
        
        assertEquals("java.lang.String", ternary.type.asClass()?.fullyQualifiedName)
        assertTrue(ternary.condition is JRBinary)
        assertTrue(ternary.truePart is JRLiteral)
        assertTrue(ternary.falsePart is JRLiteral)
    }
}
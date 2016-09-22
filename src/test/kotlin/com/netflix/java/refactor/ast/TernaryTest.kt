package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class TernaryTest(parser: Parser): AstTest(parser) {
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

        val evenOrOdd = a.classDecls[0].methods[0].body.statements[0] as Tr.VariableDecl
        val ternary = evenOrOdd.initializer as Tr.Ternary
        
        assertEquals("java.lang.String", ternary.type.asClass()?.fullyQualifiedName)
        assertTrue(ternary.condition is Tr.Binary)
        assertTrue(ternary.truePart is Tr.Literal)
        assertTrue(ternary.falsePart is Tr.Literal)
    }
}
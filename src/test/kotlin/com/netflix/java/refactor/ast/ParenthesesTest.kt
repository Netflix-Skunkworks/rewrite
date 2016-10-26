package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class ParenthesesTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            public class A {
                public void test() {
                    int n = ( 0 );
                }
            }
        """)
    }

    val variable by lazy { (a.firstMethodStatement() as Tr.VariableDecl).initializer }

    @Test
    fun parentheses() {
        assertTrue(variable is Tr.Parentheses)
    }

    @Test
    fun format() {
        assertEquals("( 0 )", variable?.printTrimmed())
    }
}
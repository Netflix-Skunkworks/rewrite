package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertTrue

abstract class EmptyTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            public class A {
                public void test() {
                    ;
                }
            }
        """)
    }

    @Test
    fun empty() {
        assertTrue(a.firstMethodStatement() is Tr.Empty)
    }

    @Test
    fun format() {
        assertEquals("", a.firstMethodStatement().printTrimmed())
    }
}
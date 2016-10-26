package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertTrue

abstract class ThrowTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            public class A {
                public void test() throws Exception {
                    throw new UnsupportedOperationException();
                }
            }
        """)
    }

    val thrown by lazy {
        a.firstMethodStatement() as Tr.Throw
    }

    @Test
    fun throwException() {
        assertTrue(thrown.exception is Tr.NewClass)
    }

    @Test
    fun format() {
        assertEquals("throw new UnsupportedOperationException()", thrown.printTrimmed())
    }
}
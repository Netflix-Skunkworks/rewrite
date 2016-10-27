package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertTrue

abstract class SynchronizedTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            public class A {
                Integer n = 0;
                public void test() {
                    synchronized(n) {
                    }
                }
            }
        """)
    }

    val sync by lazy { a.firstMethodStatement() as Tr.Synchronized }

    @Test
    fun synchronized() {
        assertTrue(sync.lock.tree is Tr.Ident)
    }

    @Test
    fun format() {
        assertEquals("synchronized(n) {\n}", sync.printTrimmed())
    }
}
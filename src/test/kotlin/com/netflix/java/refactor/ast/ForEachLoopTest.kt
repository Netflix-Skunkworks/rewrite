package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class ForEachLoopTest(parser: Parser): AstTest(parser) {

    val a by lazy { parse("""
            public class A {
                public void test() {
                    for(Integer n: new Integer[] { 0, 1 }) {
                    }
                }
            }
        """)
    }

    val forEachLoop by lazy { a.firstMethodStatement() as Tr.ForEachLoop }

    @Test
    fun forEachLoop() {
        assertTrue(forEachLoop.control.variable is Tr.VariableDecl)
        assertTrue(forEachLoop.control.iterable is Expression)
    }

    @Test
    fun format() {
        assertEquals("for(Integer n: new Integer[] { 0, 1 }) {\n}", forEachLoop.printTrimmed())
    }
}
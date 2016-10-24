package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.*

abstract class DoWhileLoopTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            |public class A {
            |    public void test() {
            |        do { } while ( true );
            |    }
            |}
        """)
    }

    val whileLoop by lazy { a.firstMethodStatement() as Tr.DoWhileLoop }

    @Test
    fun doWhileLoop() {
        assertTrue(whileLoop.condition.expr is Tr.Literal)
        assertTrue(whileLoop.body is Tr.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("do { } while ( true )", whileLoop.print())
    }
}
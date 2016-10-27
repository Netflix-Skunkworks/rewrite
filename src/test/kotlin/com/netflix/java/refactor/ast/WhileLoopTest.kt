package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.*

abstract class WhileLoopTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            |public class A {
            |    public void test() {
            |        while ( true ) { }
            |    }
            |}
        """)
    }

    val whileLoop by lazy { a.firstMethodStatement() as Tr.WhileLoop }

    @Test
    fun whileLoop() {
        assertTrue(whileLoop.condition.tree is Tr.Literal)
        assertTrue(whileLoop.body is Tr.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("while ( true ) { }", whileLoop.printTrimmed())
    }
}
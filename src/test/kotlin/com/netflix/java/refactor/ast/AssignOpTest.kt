package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class AssignOpTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            public class A {
                int n = 0;
                public void test() {
                    n += 1;
                }
            }
        """)
    }

    val assign by lazy {
        a.firstMethodStatement() as Tr.AssignOp
    }

    @Test
    fun compoundAssignment() {
        assertTrue(assign.operator is Tr.AssignOp.Operator.Addition)
    }

    @Test
    fun format() {
        assertEquals("n += 1", assign.printTrimmed())
    }
}
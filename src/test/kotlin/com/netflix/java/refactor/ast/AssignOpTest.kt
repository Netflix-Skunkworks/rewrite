package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals

abstract class AssignOpTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun compoundAssignment() {
        val a = parse("""
            public class A {
                int n = 0;
                public void test() {
                    n += 1;
                }
            }
        """)
        
        val assign = a.classDecls[0].methods[0].body.statements[0] as Tr.AssignOp
        assertEquals(Tr.AssignOp.Operator.Addition, assign.operator)
    }
}
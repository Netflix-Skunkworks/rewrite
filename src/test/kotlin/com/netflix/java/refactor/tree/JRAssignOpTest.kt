package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals

abstract class JRAssignOpTest(parser: Parser): AstTest(parser) {
    
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
        
        val assign = a.classDecls[0].methods[0].body.statements[0] as JRAssignOp
        assertEquals(JRAssignOp.Operator.Addition, assign.operator)
    }
}
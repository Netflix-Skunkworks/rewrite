package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class AssignTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun assignmentToField() {
        val a = parse("""
            public class A {
                String s;
                public void test() {
                    s = "foo";
                }
            }
        """)
        
        val assign = a.classDecls[0].methods[0].body.statements[0] as Assign
        assertEquals("s", (assign.variable as Ident).name)
        assertTrue(assign.assignment is Literal)
    }
}
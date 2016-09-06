package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class JRAssignTest(parser: Parser): AstTest(parser) {
    
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
        
        val assign = a.classDecls[0].methods[0].body.statements[0] as JRAssign
        assertEquals("s", (assign.variable as JRIdent).name)
        assertTrue(assign.assignment is JRLiteral)
    }
}
package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class SwitchTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun switch() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    case 0: break;
                    }
                }
            }
        """)
        
        val switch = a.classDecls[0].methods[0].body.statements[0] as Switch
        assertTrue(switch.selector.expr is Ident)
        assertEquals(1, switch.cases.size)
        
        val case0 = switch.cases[0]
        assertTrue(case0.pattern is Literal)
        assertTrue(case0.statements[0] is Break)
    }
    
    @Test
    fun switchWithDefault() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    default: System.out.println("default!");
                    }
                }
            }
        """)

        val switch = a.classDecls[0].methods[0].body.statements[0] as Switch
        assertTrue(switch.selector.expr is Ident)
        assertEquals(1, switch.cases.size)

        val default = switch.cases[0]
        assertNull(default.pattern)
        assertTrue(default.statements[0] is MethodInvocation)
    }
    
    @Test
    fun switchWithNoCases() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {}
                }
            }
        """)

        val switch = a.classDecls[0].methods[0].body.statements[0] as Switch
        assertTrue(switch.selector.expr is Ident)
        assertEquals(0, switch.cases.size)
    }
}
package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class JRSwitchTest(parser: Parser): AstTest(parser) {
    
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
        
        val switch = a.classDecls[0].methods[0].body.statements[0] as JRSwitch
        assertTrue(switch.selector.expr is JRIdent)
        assertEquals(1, switch.cases.size)
        
        val case0 = switch.cases[0]
        assertTrue(case0.pattern is JRLiteral)
        assertTrue(case0.statements[0] is JRBreak)
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

        val switch = a.classDecls[0].methods[0].body.statements[0] as JRSwitch
        assertTrue(switch.selector.expr is JRIdent)
        assertEquals(1, switch.cases.size)

        val default = switch.cases[0]
        assertNull(default.pattern)
        assertTrue(default.statements[0] is JRMethodInvocation)
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

        val switch = a.classDecls[0].methods[0].body.statements[0] as JRSwitch
        assertTrue(switch.selector.expr is JRIdent)
        assertEquals(0, switch.cases.size)
    }
}
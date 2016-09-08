package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class ForLoopTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun forLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for(int i = 0; i < 10; i++) {
                    }
                }
            }
        """)
        
        val forLoop = a.classDecls[0].methods[0].body.statements[0] as ForLoop
        assertEquals(1, forLoop.init.size)
        assertTrue(forLoop.condition is Binary)
        assertEquals(1, forLoop.update.size)
    }

    @Test
    fun infiniteLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for(;;) {
                    }
                }
            }
        """)

        val forLoop = a.classDecls[0].methods[0].body.statements[0] as ForLoop
        assertEquals(0, forLoop.init.size)
        assertNull(forLoop.condition)
        assertEquals(0, forLoop.update.size)
    }
}
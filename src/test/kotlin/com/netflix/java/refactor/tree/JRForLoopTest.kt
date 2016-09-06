package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class JRForLoopTest(parser: Parser): AstTest(parser) {
    
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
        
        val forLoop = a.classDecls[0].methods[0].body.statements[0] as JRForLoop
        assertEquals(1, forLoop.init.size)
        assertTrue(forLoop.condition is JRBinary)
        assertEquals(1, forLoop.update.size)
        assertNull(forLoop.type)
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

        val forLoop = a.classDecls[0].methods[0].body.statements[0] as JRForLoop
        assertEquals(0, forLoop.init.size)
        assertNull(forLoop.condition)
        assertEquals(0, forLoop.update.size)
    }
}
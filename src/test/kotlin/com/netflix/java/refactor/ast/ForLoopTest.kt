package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.*
import org.junit.Test

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
        
        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertEquals(1, forLoop.control.init.size)
        assertTrue(forLoop.control.condition is Tr.Binary)
        assertEquals(1, forLoop.control.update.size)
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

        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertTrue(forLoop.control.init[0] is Tr.Empty)
        assertTrue(forLoop.control.condition is Tr.Empty)
        assertTrue(forLoop.control.update[0] is Tr.Empty)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public void test() {
                    for ( int i = 0 ; i < 10 ; i++ ) {
                    }
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertEquals("for ( int i = 0 ; i < 10 ; i++ ) {\n}", forLoop.print())
    }

    @Test
    fun formatInfiniteLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for ( ; ; ) {}
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertEquals("for ( ; ; ) {}", forLoop.print())
    }

    @Test
    fun formatLoopNoInit() {
        val a = parse("""
            public class A {
                public void test() {
                    int i = 0;
                    for ( ; i < 10 ; i++ ) {}
                }
            }
        """)

        val forLoop = a.classDecls[0].methods()[0].body!!.statements[1] as Tr.ForLoop
        assertEquals("for ( ; i < 10 ; i++ ) {}", forLoop.print())
    }

    @Test
    fun formatLoopNoCondition() {
        val a = parse("""
            public class A {
                public void test() {
                    int i = 0;
                    for(; i < 10; i++) {}
                }
            }
        """)

        val forLoop = a.classDecls[0].methods()[0].body!!.statements[1] as Tr.ForLoop
        assertEquals("for(; i < 10; i++) {}", forLoop.print())
    }
}
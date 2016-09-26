package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class SynchronizedTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun synchronized() {
        val a = parse("""
            public class A {
                Integer n = 0;
                public void test() {
                    synchronized(n) {
                        System.out.println("locked");
                    }
                }
            }
        """)
        
        val sync = a.classDecls[0].methods()[0].body.statements[0] as Tr.Synchronized
        assertTrue(sync.lock.expr is Tr.Ident)
    }
}
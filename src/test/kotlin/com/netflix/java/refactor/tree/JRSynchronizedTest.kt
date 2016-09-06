package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRSynchronizedTest(parser: Parser): AstTest(parser) {
    
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
        
        val sync = a.classDecls[0].methods[0].body.statements[0] as JRSynchronized
        assertTrue(sync.lock is JRIdent)
    }
}
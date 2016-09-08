package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class TryTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun tryFinally() {
        val a = parse("""
            public class A {
                public void test() {
                    try {
                    }
                    finally {
                    }
                }
            }
        """)
        
        val tryable = a.classDecls[0].methods[0].body.statements[0] as Try
        assertTrue(tryable.body is Block)
        assertEquals(0, tryable.catchers.size)
        assertTrue(tryable.finally is Block)
    }
    
    @Test
    fun tryCatchNoFinally() {
        val a = parse("""
            public class A {
                public void test() {
                    try {
                    }
                    catch(Throwable t) {
                    }
                }
            }
        """)

        val tryable = a.classDecls[0].methods[0].body.statements[0] as Try
        assertEquals(1, tryable.catchers.size)
    }
    
    @Test
    fun tryWithResources() {
        val a = parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {
                    }
                    catch(IOException e) {
                    }
                }
            }
        """)

        val tryable = a.classDecls[0].methods[0].body.statements[0] as Try
        assertEquals(1, tryable.resources.size)
    }
}
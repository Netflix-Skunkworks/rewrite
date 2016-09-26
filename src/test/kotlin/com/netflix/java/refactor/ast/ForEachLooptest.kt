package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class ForEachLoopTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun forEachLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for(String s: new String[] { "a", "b" }) {
                    }
                }
            }
        """)

        val forEachLoop = a.classDecls[0].methods()[0].body.statements[0] as Tr.ForEachLoop
        assertTrue(forEachLoop.variable is Tr.VariableDecl)
        assertTrue(forEachLoop.iterable is Expression)
    }
}
package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRForEachLoopTest(parser: Parser): AstTest(parser) {
    
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

        val forEachLoop = a.classDecls[0].methods[0].body.statements[0] as JRForEachLoop
        assertTrue(forEachLoop.variable is JRVariableDecl)
        assertTrue(forEachLoop.iterable is JRExpression)
    }
}
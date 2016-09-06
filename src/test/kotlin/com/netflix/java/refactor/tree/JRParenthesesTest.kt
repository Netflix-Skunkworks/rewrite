package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRParenthesesTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun parentheses() {
        val a = parse("""
            public class A {
                public void test() {
                    int n = (0);
                }
            }
        """)
        
        val variable = a.classDecls[0].methods[0].body.statements[0] as JRVariableDecl
        assertTrue(variable.initializer is JRParentheses)
    }
}
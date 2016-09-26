package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class ParenthesesTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun parentheses() {
        val a = parse("""
            public class A {
                public void test() {
                    int n = (0);
                }
            }
        """)
        
        val variable = a.classDecls[0].methods()[0].body.statements[0] as Tr.VariableDecl
        assertTrue(variable.initializer is Tr.Parentheses)
    }
}
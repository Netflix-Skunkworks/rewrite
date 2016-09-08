package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class ArrayAccessTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun arrayAccess() {
        val a = parse("""
            public class a {
                int n[] = new int[] { 0 };
                public void test() {
                    int m = n[0];
                }
            }
        """)
        
        val variable = a.classDecls[0].methods[0].body.statements[0] as VariableDecl
        val arrAccess = variable.initializer as ArrayAccess
        assertTrue(arrAccess.indexed is Ident)
        assertTrue(arrAccess.index is Literal)
    }
}
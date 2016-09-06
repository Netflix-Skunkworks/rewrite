package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRArrayAccessTest(parser: Parser): AstTest(parser) {
    
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
        
        val variable = a.classDecls[0].methods[0].body.statements[0] as JRVariableDecl
        val arrAccess = variable.initializer as JRArrayAccess
        assertTrue(arrAccess.indexed is JRIdent)
        assertTrue(arrAccess.index is JRLiteral)
    }
}
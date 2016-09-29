package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals

abstract class MethodDeclTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun methodDecl() {
        val a = parse("""
            public class A {
                public <P> P foo(P p, String s, String... args) {
                    return p;
                }
            }
        """)
        
        val meth = a.classDecls[0].methods()[0]
        assertEquals("foo", meth.name.name)
        assertEquals(3, meth.params.size)
        assertEquals(1, meth.body.statements.size)
        assertEquals("P", ((meth.returnTypeExpr as Tr.Ident).type as Type.GenericTypeVariable).name)
    }
    
    @Test
    fun format() {
        val a = parse("""
            public class A {
                public <P> P foo(P p, String s, String... args) { return p; }
            }
        """)

        val meth = a.classDecls[0].methods()[0]
        assertEquals("public <P> P foo(P p, String s, String... args) { return p; }", meth.print())
    }
}
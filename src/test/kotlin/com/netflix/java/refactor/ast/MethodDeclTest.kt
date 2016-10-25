package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

abstract class MethodDeclTest(parser: Parser): AstTest(parser) {

    @Test
    fun constructor() {
        val a = parse("""
            package a;
            public class A {
                public A() { }
            }
        """)

        assertNull(a.typeDecls[0].methods()[0].returnTypeExpr)
    }

    @Test
    fun methodDecl() {
        val a = parse("""
            public class A {
                public <P> P foo(P p, String s, String... args) {
                    return p;
                }
            }
        """)
        
        val meth = a.typeDecls[0].methods()[0]
        assertEquals("foo", meth.name.name)
        assertEquals(3, meth.params.params.size)
        assertEquals(1, meth.body!!.statements.size)
        assertEquals("P", ((meth.returnTypeExpr as Tr.Ident).type as Type.GenericTypeVariable).name)
    }
    
    @Test
    fun format() {
        val a = parse("""
            public class A {
                public <P> P foo(P p, String s, String ... args) { return p; }
            }
        """)

        val meth = a.typeDecls[0].methods()[0]
        assertEquals("public <P> P foo(P p, String s, String ... args) { return p; }", meth.print())
    }

    @Test
    fun formatConstructor() {
        val a = parse("""
            package a;
            public class A {
                public A() { }
            }
        """)

        val meth = a.typeDecls[0].methods()[0]
        assertEquals("public A() { }", meth.print())
    }
}

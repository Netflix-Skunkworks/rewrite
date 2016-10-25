package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

abstract class VariableDeclTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun fieldDefinition() {
        val a = parse("""
            public class A {
                String a = "";
            }
        """)
        
        val varDecl = a.fields()[0]
        assertTrue(varDecl.varType is Tr.Ident)
        assertEquals("a", varDecl.name.name)
        assertEquals("java.lang.String", varDecl.type.asClass()?.fullyQualifiedName)
        assertEquals((varDecl.varType as Tr.Ident).type, varDecl.type)
        assertTrue(varDecl.initializer is Tr.Literal)
    }

    @Test
    fun localVariableDefinition() {
        val a = parse("""
            public class A {
                public void test() {
                    String a = "";
                }
            }
        """)

        val varDecl = a.firstMethodStatement() as Tr.VariableDecl
        assertEquals("java.lang.String", varDecl.type.asClass()?.fullyQualifiedName)
        assertEquals("a", varDecl.name.name)
    }

    @Test
    fun fieldWithNoInitializer() {
        val a = parse("""
            public class A {
                String a;
            }
        """)

        val varDecl = a.fields()[0]
        assertNull(varDecl.initializer)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public static int n = 0;
            }
        """)
        
        val varDecl = a.fields()[0]
        assertEquals("public static int n = 0", varDecl.print())
    }

    @Test
    fun formatArrayVariables() {
        val a = parse("""
            |public class A {
            |   int n [ ];
            |   String s [ ] [ ];
            |   int [ ] n2;
            |   String [ ] [ ] s2;
            |}
        """)

        val (n, s, n2, s2) = a.fields(0..3)

        assertEquals("int n [ ]", n.print())
        assertEquals("String s [ ] [ ]", s.print())
        assertEquals("int [ ] n2", n2.print())
        assertEquals("String [ ] [ ] s2", s2.print())
    }
}
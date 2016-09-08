package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class VariableDeclTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun fieldDefinition() {
        val a = parse("""
            public class A {
                String a = "";
            }
        """)
        
        val varDecl = a.classDecls[0].fields[0]
        assertTrue(varDecl.varType is Ident)
        assertEquals("a", varDecl.name)
        assertEquals("java.lang.String", varDecl.type.asClass()?.fullyQualifiedName)
        assertEquals((varDecl.varType as Ident).type, varDecl.type)
        assertTrue(varDecl.initializer is Literal)
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

        val varDecl = a.classDecls[0].methods[0].body.statements[0] as VariableDecl
        assertEquals("java.lang.String", varDecl.type.asClass()?.fullyQualifiedName)
        assertEquals("a", varDecl.name)
    }

    @Test
    fun fieldWithNoInitializer() {
        val a = parse("""
            public class A {
                String a;
            }
        """)

        val varDecl = a.classDecls[0].fields[0]
        assertNull(varDecl.initializer)
    }
}
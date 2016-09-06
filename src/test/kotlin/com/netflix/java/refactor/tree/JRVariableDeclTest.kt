package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class JRVariableDeclTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun fieldDefinition() {
        val a = parse("""
            public class A {
                String a = "";
            }
        """)
        
        val varDecl = a.classDecls[0].fields[0]
        assertTrue(varDecl.varType is JRIdent)
        assertEquals("a", varDecl.name)
        assertEquals("java.lang.String", varDecl.type.asClass()?.fullyQualifiedName)
        assertEquals((varDecl.varType as JRIdent).type, varDecl.type)
        assertTrue(varDecl.initializer is JRLiteral)
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

        val varDecl = a.classDecls[0].methods[0].body.statements[0] as JRVariableDecl
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
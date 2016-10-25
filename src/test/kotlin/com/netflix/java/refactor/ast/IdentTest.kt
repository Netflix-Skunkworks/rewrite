package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals

abstract class IdentTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun referToField() {
        val a = parse("""
            public class A {
                Integer n = 0;
                Integer m = n;
            }
        """)
        
        val ident = a.typeDecls[0].fields().first { it.name.name == "m" }.initializer as Tr.Ident
        assertEquals("n", ident.name)
        assertEquals("java.lang.Integer", ident.type.asClass()?.fullyQualifiedName)
    }
}
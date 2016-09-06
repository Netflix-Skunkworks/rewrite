package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals

abstract class JRIdentTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun referToField() {
        val a = parse("""
            public class A {
                Integer n = 0;
                Integer m = n;
            }
        """)
        
        val ident = a.classDecls[0].fields.first { it.name == "m" }.initializer as JRIdent
        assertEquals("n", ident.name)
        assertEquals("java.lang.Integer", ident.type.asClass()?.fullyQualifiedName)
    }
}
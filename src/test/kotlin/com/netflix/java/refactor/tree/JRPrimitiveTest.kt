package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals

abstract class JRPrimitiveTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun primitiveField() {
        val a = parse("""
            public class A {
                int n = 0;
            }
        """)
        
        val primitive = a.classDecls[0].fields[0].varType as JRPrimitive
        assertEquals(JRType.Tag.Int, primitive.typeTag)
    }
}
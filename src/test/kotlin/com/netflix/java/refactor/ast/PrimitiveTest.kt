package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals

abstract class PrimitiveTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun primitiveField() {
        val a = parse("""
            public class A {
                int n = 0;
            }
        """)
        
        val primitive = a.classDecls[0].fields()[0].varType as Tr.Primitive
        assertEquals(Type.Tag.Int, primitive.typeTag)
    }
}
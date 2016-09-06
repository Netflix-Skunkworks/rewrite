package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class JRNewArrayTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun newArray() {
        val a = parse("""
            public class A {
                int[] n = new int[0];
            }
        """)
        
        val newArr = a.classDecls[0].fields[0].initializer as JRNewArray
        assertTrue(newArr.elements.isEmpty())
        assertTrue(newArr.type is JRPrimitive)
        assertEquals(1, newArr.dimensions.size)
        assertTrue(newArr.dimensions[0] is JRLiteral)
    }

    @Test
    fun newArrayWithElements() {
        val a = parse("""
            public class A {
                int[] n = new int[] { 0, 1, 2 };
            }
        """)

        val newArr = a.classDecls[0].fields[0].initializer as JRNewArray
        assertTrue(newArr.dimensions.isEmpty())
        assertTrue(newArr.type is JRPrimitive)
        assertEquals(3, newArr.elements.size)
        newArr.dimensions.forEach { 
            assertTrue(it is JRLiteral)
        }
    }
}
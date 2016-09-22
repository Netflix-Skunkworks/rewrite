package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class NewArrayTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun newArray() {
        val a = parse("""
            public class A {
                int[] n = new int[0];
            }
        """)
        
        val newArr = a.classDecls[0].fields[0].initializer as Tr.NewArray
        assertTrue(newArr.elements.isEmpty())
        assertTrue(newArr.type is Type.Array)
        assertTrue(newArr.type.asArray()?.elemType is Type.Primitive)
        assertEquals(1, newArr.dimensions.size)
        assertTrue(newArr.dimensions[0] is Tr.Literal)
    }

    @Test
    fun newArrayWithElements() {
        val a = parse("""
            public class A {
                int[] n = new int[] { 0, 1, 2 };
            }
        """)

        val newArr = a.classDecls[0].fields[0].initializer as Tr.NewArray
        assertTrue(newArr.dimensions.isEmpty())
        assertTrue(newArr.type is Type.Array)
        assertTrue(newArr.type.asArray()?.elemType is Type.Primitive)
        assertEquals(3, newArr.elements.size)
        newArr.dimensions.forEach { 
            assertTrue(it is Tr.Literal)
        }
    }
}
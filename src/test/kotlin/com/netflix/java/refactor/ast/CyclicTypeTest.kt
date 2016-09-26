package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

abstract class CyclicTypeTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun cyclicType() {
        val a = parse("""
            public class A {
                A[] nested = new A[0];
            }
        """)
        
        val fieldType = a.classDecls[0].fields()[0].type.asArray()
        assertTrue(fieldType is Type.Array)

        val elemType = fieldType!!.elemType.asClass()
        assertTrue(elemType is Type.Class)
        
        assertTrue(elemType!!.members[0].type?.isCyclicRef() ?: false)
    }
}
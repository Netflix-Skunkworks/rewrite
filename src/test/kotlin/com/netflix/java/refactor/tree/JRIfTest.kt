package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class JRIfTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun ifElse() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) {
                    } 
                    else if(n == 1) {
                    }
                    else {
                    }
                }
            }
        """)
        
        val iff = a.classDecls[0].methods[0].body.statements[0] as JRIf
        assertTrue(iff.ifCondition is JRBinary)
        assertTrue(iff.thenPart is JRBlock)
        
        assertTrue(iff.elsePart is JRIf)
        val elseIf = iff.elsePart as JRIf
        assertTrue(elseIf.ifCondition is JRBinary)
        assertTrue(elseIf.thenPart is JRBlock)
        assertTrue(elseIf.elsePart is JRBlock)
    }
    
    @Test
    fun noElse() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) {} 
                }
            }
        """)
        
        val iff = a.classDecls[0].methods[0].body.statements[0] as JRIf
        assertNull(iff.elsePart)
    }
}
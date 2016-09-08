package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class IfTest(parser: Parser): AstTest(parser) {
    
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
        
        val iff = a.classDecls[0].methods[0].body.statements[0] as If
        assertTrue(iff.ifCondition.expr is Binary)
        assertTrue(iff.thenPart is Block)
        
        assertTrue(iff.elsePart is If)
        val elseIf = iff.elsePart as If
        assertTrue(elseIf.ifCondition.expr is Binary)
        assertTrue(elseIf.thenPart is Block)
        assertTrue(elseIf.elsePart is Block)
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
        
        val iff = a.classDecls[0].methods[0].body.statements[0] as If
        assertNull(iff.elsePart)
    }
}
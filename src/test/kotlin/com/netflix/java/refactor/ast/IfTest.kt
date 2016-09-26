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
        
        val iff = a.classDecls[0].methods()[0].body.statements[0] as Tr.If
        assertTrue(iff.ifCondition.expr is Tr.Binary)
        assertTrue(iff.thenPart is Tr.Block)
        
        assertTrue(iff.elsePart is Tr.If)
        val elseIf = iff.elsePart as Tr.If
        assertTrue(elseIf.ifCondition.expr is Tr.Binary)
        assertTrue(elseIf.thenPart is Tr.Block)
        assertTrue(elseIf.elsePart is Tr.Block)
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
        
        val iff = a.classDecls[0].methods()[0].body.statements[0] as Tr.If
        assertNull(iff.elsePart)
    }
}
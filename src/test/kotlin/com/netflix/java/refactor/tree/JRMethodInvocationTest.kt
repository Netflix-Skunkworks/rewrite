package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class JRMethodInvocationTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun methodInvocation() {
        val a = parse("""
            public class A {
                Integer n = foo(0, 1, 2);
            
                public Integer foo(Integer n, Integer... ns) {
                    return n;
                }
            }
        """)

        val inv = a.classDecls[0].fields[0].initializer as JRMethodInvocation

        // check assumptions about the call site
        assertEquals("foo", inv.methodSelect.source)
        assertEquals("java.lang.Integer", inv.returnType().asClass().fullyQualifiedName)
        assertEquals(listOf(JRType.Tag.Int, JRType.Tag.Int, JRType.Tag.Int),
                inv.args.filterIsInstance<JRLiteral>().map { it.typeTag })
        
        val effectParams = inv.resolvedSignature!!.paramTypes
        assertEquals("java.lang.Integer", effectParams[0].asClass().fullyQualifiedName)
        assertTrue(effectParams[1].isArrayOfType("java.lang.Integer"))

        // check assumptions about the target method
        // notice how for non-generic method signatures, genericSignature and resolvedSignature match
        val methType = inv.genericSignature!!
        assertEquals("java.lang.Integer", methType.returnType.asClass().fullyQualifiedName)
        assertEquals("java.lang.Integer", methType.paramTypes[0].asClass().fullyQualifiedName)
        assertTrue(methType.paramTypes[1].isArrayOfType("java.lang.Integer"))

        assertEquals("A", inv.declaringType?.fullyQualifiedName)
    }
    
    @Test
    fun genericMethodInvocation() {
        val a = parse("""
            public class A {
                Integer n = foo(0, 1, 2);
            
                public <T> T foo(T t, T... ts) {
                    return t;
                }
            }
        """)
        
        val inv = a.classDecls[0].fields[0].initializer as JRMethodInvocation
        
        // check assumptions about the call site
        assertEquals("foo", inv.methodSelect.source)
        assertEquals("java.lang.Integer", inv.returnType().asClass().fullyQualifiedName)
        assertEquals(listOf(JRType.Tag.Int, JRType.Tag.Int, JRType.Tag.Int),
                inv.args.filterIsInstance<JRLiteral>().map { it.typeTag })
        val effectParams = inv.resolvedSignature!!.paramTypes
        assertEquals("java.lang.Integer", effectParams[0].asClass().fullyQualifiedName)
        assertTrue(effectParams[1].isArrayOfType("java.lang.Integer"))
        
        // check assumptions about the target method
        // notice how, in the case of generic arguments, the generics are concretized to match the call site
        val methType = inv.genericSignature!!
        assertEquals("T", methType.returnType.asGeneric().name)
        assertEquals("T", methType.paramTypes[0].asGeneric().name)
        assertTrue(methType.paramTypes[1].isArrayOfType("T"))

        assertEquals("A", inv.declaringType?.fullyQualifiedName)
    }
    
    @Test
    fun staticMethodInvocation() {
        val a = parse("""
            public class A {
                Integer n = staticFoo(0);
                
                public static int staticFoo(int arg) {
                    return arg;
                }
            }
        """)

        val inv = a.classDecls[0].fields[0].initializer as JRMethodInvocation
        assertEquals("staticFoo", inv.methodSelect.source)
        assertEquals("A", inv.declaringType?.fullyQualifiedName)
    }
}
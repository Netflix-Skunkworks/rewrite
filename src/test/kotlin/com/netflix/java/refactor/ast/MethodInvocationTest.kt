package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

abstract class MethodInvocationTest(parser: Parser) : AstTest(parser) {

    val a by lazy {
        parse("""
            public class A {
                Integer m = foo ( 0, 1, 2 );
                Integer n = staticFoo ( 0 );
                Integer o = generic ( 0, 1, 2 );
                Integer p = this. < Integer > generic ( 0, 1, 2 );
                Integer q = staticFoo ( );

                public static int staticFoo(int... args) { return arg; }
                public Integer foo(Integer n, Integer... ns) { return n; }
                public <T> T generic(T n, T... ns) { return n; }
            }
        """)
    }

    val allInvs by lazy { a.fields(0..4).map { it.initializer as Tr.MethodInvocation } }

    val inv by lazy { allInvs[0] }
    val staticInv by lazy { allInvs[1] }
    val genericInv by lazy { allInvs[2] }
    val explicitGenericInv by lazy { allInvs[3] }
    val parameterlessStaticInv by lazy { allInvs[4] }

    @Test
    fun methodInvocation() {
        // check assumptions about the call site
        assertEquals("foo", inv.name.print())
        assertEquals("java.lang.Integer", inv.returnType().asClass()?.fullyQualifiedName)
        assertEquals(listOf(Type.Tag.Int, Type.Tag.Int, Type.Tag.Int),
                inv.args.args.filterIsInstance<Tr.Literal>().map { it.typeTag })

        val effectParams = inv.resolvedSignature!!.paramTypes
        assertEquals("java.lang.Integer", effectParams[0].asClass()?.fullyQualifiedName)
        assertTrue(effectParams[1].isArrayOfType("java.lang.Integer"))

        // check assumptions about the target method
        // notice how for non-generic method signatures, genericSignature and resolvedSignature match
        val methType = inv.genericSignature!!
        assertEquals("java.lang.Integer", methType.returnType.asClass()?.fullyQualifiedName)
        assertEquals("java.lang.Integer", methType.paramTypes[0].asClass()?.fullyQualifiedName)
        assertTrue(methType.paramTypes[1].isArrayOfType("java.lang.Integer"))

        assertEquals("A", inv.declaringType?.fullyQualifiedName)
    }

    @Test
    fun genericMethodInvocation() {
        listOf(genericInv, explicitGenericInv).forEach { test ->
            // check assumptions about the call site
            assertEquals("java.lang.Integer", test.returnType().asClass()?.fullyQualifiedName)
            assertEquals(listOf(Type.Tag.Int, Type.Tag.Int, Type.Tag.Int),
                    test.args.args.filterIsInstance<Tr.Literal>().map { it.typeTag })

            val effectiveParams = test.resolvedSignature!!.paramTypes
            assertEquals("java.lang.Integer", effectiveParams[0].asClass()?.fullyQualifiedName)
            assertTrue(effectiveParams[1].isArrayOfType("java.lang.Integer"))

            // check assumptions about the target method
            // notice how, in the case of generic arguments, the generics are concretized to match the call site
            val methType = test.genericSignature!!
            assertEquals("T", methType.returnType.asGeneric()?.name)
            assertEquals("T", methType.paramTypes[0].asGeneric()?.name)
            assertTrue(methType.paramTypes[1].isArrayOfType("T"))
        }
    }

    @Test
    fun staticMethodInvocation() {
        assertEquals("staticFoo", staticInv.name.print())
        assertEquals("A", staticInv.declaringType?.fullyQualifiedName)
    }

    @Test
    fun format() {
        assertEquals("foo ( 0, 1, 2 )", inv.print())
        assertEquals("staticFoo ( 0 )", staticInv.print())
        assertEquals("this. < Integer > generic ( 0, 1, 2 )", explicitGenericInv.print())
        assertEquals("staticFoo ( )", parameterlessStaticInv.print())
    }

    @Test
    fun methodThatDoesNotExist() {
        val a = parse("""
            public class A {
                Integer n = doesNotExist();
            }
        """)

        val inv = a.fields()[0].initializer as Tr.MethodInvocation
        assertEquals("A", inv.declaringType?.fullyQualifiedName)
        assertNull(inv.resolvedSignature)
        assertNull(inv.genericSignature)
    }
}
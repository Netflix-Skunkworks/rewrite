package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

abstract class ClassDeclTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun multipleClassDeclarationsInOneCompilationUnit() {
        val a = parse("""
            public class A {}
            class B {}
        """)

        assertEquals(listOf("A", "B"), a.typeDecls.map { it.name.name }.sorted())
    }
    
    @Test
    fun fields() {
        val a = parse("""
            import java.util.*;
            public class A {
                List l;
            }
        """)

        assertEquals(1, a.typeDecls[0].fields().size)
    }

    @Test
    fun methods() {
        val a = parse("""
            public class A {
                public void fun() {}
            }
        """)

        assertEquals(1, a.typeDecls[0].methods().size)
    }
    
    @Test
    fun implements() {
        val b = "public interface B {}"
        val a = "public class A implements B {}"
        
        assertEquals(1, parse(a, whichDependsOn = b).typeDecls[0].implements.size)
    }

    @Test
    fun extends() {
        val b = "public class B {}"
        val a = "public class A extends B {}"

        val aClass = parse(a, whichDependsOn = b).typeDecls[0] as Tr.ClassDecl
        assertNotNull(aClass.extends)
    }

    @Test
    fun format() {
        val b = "public class B<T> {}"
        val a = "@Deprecated public class A < T > extends B < T > {}"

        val aClass = parse(a, whichDependsOn = b).typeDecls[0]
        assertEquals(a, aClass.print())
    }
}
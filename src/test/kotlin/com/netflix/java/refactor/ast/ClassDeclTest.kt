package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

abstract class ClassDeclTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun multipleClassDeclarationsInOneCompilationUnit() {
        val a = parse("""
            public class A {}
            class B {}
        """)

        assertEquals(listOf("A", "B"), a.classDecls.map { it.name }.sorted())
    }
    
    @Test
    fun fields() {
        val a = parse("""
            import java.util.*;
            public class A {
                List l;
            }
        """)

        assertEquals(1, a.classDecls[0].fields().size)
    }

    @Test
    fun methods() {
        val a = parse("""
            public class A {
                public void fun() {}
            }
        """)

        assertEquals(1, a.classDecls[0].methods().size)
    }
    
    @Test
    fun implements() {
        val b = "public interface B {}"
        val a = "public class A implements B {}"
        
        assertEquals(1, parse(a, whichDependsOn = b).classDecls[0].implements.size)
    }

    @Test
    fun extends() {
        val b = "public class B {}"
        val a = "public class A extends B {}"

        assertNotNull(parse(a, whichDependsOn = b).classDecls[0].extends)
    }
}
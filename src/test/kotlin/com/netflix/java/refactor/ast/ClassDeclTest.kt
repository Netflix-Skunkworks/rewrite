package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

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

        val aClass = parse(a, whichDependsOn = b).typeDecls[0]
        assertNotNull(aClass.extends)
    }

    @Test
    fun format() {
        val b = "public class B<T> {}"
        val a = "@Deprecated public class A < T > extends B < T > {}"
        assertEquals(a, parse(a, whichDependsOn = b).typeDecls[0].printTrimmed())
    }

    @Test
    fun formatInterface() {
        val b = "public interface B {}"
        val a = "public interface A extends B {}"
        assertEquals(a, parse(a, whichDependsOn = b).typeDecls[0].printTrimmed())
    }

    @Test
    fun formatAnnotation() {
        val a = "public @interface Produces { }"
        assertEquals(a, parse(a).typeDecls[0].printTrimmed())
    }

    @Test
    fun trailingLastTypeDeclaration() {
        val a = parse("public class A {}/**/")
        assertEquals("/**/", (a.typeDecls[0].formatting as Formatting.Reified).suffix)
    }

    @Test
    fun enumWithParameters() {
        val aSrc = """
            |public enum A {
            |    ONE(1),
            |    TWO(2);
            |
            |    A(int n) {}
            |}
        """.trimMargin()

        val a = parse(aSrc)

        Assert.assertTrue(a.typeDecls[0].kind is Tr.ClassDecl.Kind.Enum)
        assertEquals("ONE(1)", a.typeDecls[0].enumValues()[0].printTrimmed())
        assertEquals(aSrc, a.printTrimmed())
    }

    @Test
    fun enumWithoutParameters() {
        val aSrc = "public enum A { ONE, TWO }"
        assertEquals(aSrc, parse(aSrc).typeDecls[0].printTrimmed())
    }

    @Test
    fun enumWithEmptyParameters() {
        val aSrc = "public enum A { ONE(), TWO() }"
        assertEquals(aSrc, parse(aSrc).typeDecls[0].printTrimmed())
    }
}
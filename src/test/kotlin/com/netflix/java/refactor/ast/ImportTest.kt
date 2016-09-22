package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class ImportTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun matchImport() {
        val a = parse("""
            import java.util.List;
            public class A {}
        """)

        assertTrue(a.imports.first().matches("java.util.List", a))
    }

    @Test
    fun matchStarImport() {
        val a = parse("""
            import java.util.*;
            public class A {}
        """)

        assertTrue(a.imports.first().matches("java.util.List", a))
    }

    @Test
    fun hasStarImportOnInnerClass() {
        val a = """
            package a;
            public class A {
               public static class B { }
            }
        """

        val c = """
            import a.*;
            public class C {
                A.B b = new A.B();
            }
        """

        val cu = parse(c, whichDependsOn = a)
        val import = cu.imports.first()
        assertTrue(import.matches("a.A.B", cu))
        assertTrue(import.matches("a.A", cu))
    }
    
    @Test
    fun buildImport() {
        val a = parse("public class A {}")
        
        val import = Tr.Import.build("java.util.List")
        
        assertEquals("import java.util.List;", import.source.text(a))
        assertEquals("java.util.List", import.qualid.source.text(a))
        assertEquals("List", import.qualid.fieldName)
    }
    
    @Test
    fun buildImportOnInnerClass() {
        val a = parse("public class A {}")

        val import = Tr.Import.build("a.Outer.Inner")

        assertEquals("import a.Outer.Inner;", import.source.text(a))
        assertEquals("a.Outer.Inner", import.qualid.source.text(a))
        
        val inner = import.qualid
        assertEquals("Inner", inner.fieldName)
        assertEquals("a.Outer.Inner", inner.type.asClass()?.fullyQualifiedName)

        val outer = inner.target as Tr.FieldAccess
        assertEquals("Outer", outer.fieldName)
        assertEquals("a.Outer", outer.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun buildStaticImport() {
        val a = parse("public class B {}")

        val import = Tr.Import.build("a.A.*", static = true)

        assertEquals("import static a.A.*;", import.source.text(a))
        assertEquals("a.A.*", import.qualid.source.text(a))
        assertEquals("*", import.qualid.fieldName)
    }
}
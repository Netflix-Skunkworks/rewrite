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

        assertTrue(a.imports.first().matches("java.util.List"))
    }

    @Test
    fun matchStarImport() {
        val a = parse("""
            import java.util.*;
            public class A {}
        """)

        assertTrue(a.imports.first().matches("java.util.List"))
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
        assertTrue(import.matches("a.A.B"))
        assertTrue(import.matches("a.A"))
    }
    
    @Test
    fun buildImport() {
        val import = Tr.Import.build("java.util.List")
        
        assertEquals("import java.util.List", import.print())
        assertEquals("java.util.List", import.qualid.print())
        assertEquals("List", import.qualid.fieldName)
    }
    
    @Test
    fun buildImportOnInnerClass() {
        val import = Tr.Import.build("a.Outer.Inner")

        assertEquals("import a.Outer.Inner", import.print())
        assertEquals("a.Outer.Inner", import.qualid.print())
        
        val inner = import.qualid
        assertEquals("Inner", inner.fieldName)
        assertEquals("a.Outer.Inner", inner.type.asClass()?.fullyQualifiedName)

        val outer = inner.target as Tr.FieldAccess
        assertEquals("Outer", outer.fieldName)
        assertEquals("a.Outer", outer.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun buildStaticImport() {
        val import = Tr.Import.build("a.A.*", static = true)

        assertEquals("import static a.A.*", import.print())
        assertEquals("a.A.*", import.qualid.print())
        assertEquals("*", import.qualid.fieldName)
    }
    
    @Test
    fun format() {
        val a = parse("""
            |import java.util.List;
            |import static java.util.Collections.*;
            |public class A {}
        """)
        
        // FIXME scan is getting called too late, so currentPath is missing path elements
        
        assertEquals("import java.util.List", a.imports[0].print())
        assertEquals("import static java.util.Collections.*", a.imports[1].print())
    }
}
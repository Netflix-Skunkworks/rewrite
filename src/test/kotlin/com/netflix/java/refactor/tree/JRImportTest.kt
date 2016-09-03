package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertTrue

abstract class JRImportTest(parser: Parser): AstTest(parser) {
    
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

        val import = parse(c, whichDependsOn = a).imports.first()
        assertTrue(import.matches("a.A.B"))
        assertTrue(import.matches("a.A"))
    }
}
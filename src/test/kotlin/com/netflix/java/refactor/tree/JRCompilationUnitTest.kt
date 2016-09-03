package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals

abstract class JRCompilationUnitTest(parser: Parser): AstTest(parser) {
    @Test
    fun imports() {
        val a = parse("""
            import java.util.List;
            import java.io.*;
            public class A {}
        """)

        assertEquals(2, a.imports.size)
    }

    @Test
    fun classes() {
        val a = parse("""
            public class A {}
            class B{}
        """)

        assertEquals(2, a.classDecls.size)
    }
}
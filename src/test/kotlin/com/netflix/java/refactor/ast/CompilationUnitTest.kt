package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class CompilationUnitTest(parser: Parser): AstTest(parser) {
    
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

    @Test
    fun recordGitStyleDiff() {
        val (b, c) = listOf("B", "C").map {
            """
                |public class $it {
                |    public void foo(int i) {}
                |}
            """
        }

        val a = """
            |public class A {
            |   public void test() {
            |      B local = new B();
            |      local.foo(0);
            |   }
            |}
            |
        """

        val cu = parse(a, b, c)
        val diff1 = cu.beginDiff()

        val diff2 = cu.diff {
            refactor().changeType("B", "C").fix()
        }

        val expectedDiff = """
            |diff --git a/${cu.source.path} b/${cu.source.path}
            |index 70f03ee..b82f543 100644
            |--- a/${cu.source.path}
            |+++ b/${cu.source.path}
            |@@ -1,6 +1,6 @@
            | public class A {
            |    public void test() {
            |-      B local = new B();
            |+      C local = new C();
            |       local.foo(0);
            |    }
            | }
            |
        """.trimMargin()

        assertEquals(expectedDiff, diff1.gitStylePatch())
        assertEquals(expectedDiff, diff2)
    }
}
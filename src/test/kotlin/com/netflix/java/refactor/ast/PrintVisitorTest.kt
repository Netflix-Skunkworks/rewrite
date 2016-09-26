package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals

class PrintVisitorTest: AstTest(OracleJdkParser()) {

    @Test
    fun printClassDeclWithNewImport() {
        val a = parse("""
            |import java.util.List;
            |
            |public class A {
            |}
        """)

        val mut = AstTransform(Cursor(listOf(a)), { cu: Tr.CompilationUnit ->
            cu.copy(imports = cu.imports + Tr.Import.build("java.util.Set"))
        })

        val a2 = TransformVisitor(listOf(mut)).visit(a) as Tr.CompilationUnit

        assertEquals(a2.print(), """
            |import java.util.List;
            |
            |public class A {
            |}
        """)
    }
}
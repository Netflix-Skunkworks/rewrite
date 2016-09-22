package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.refactor.fix.PositionCorrectingVisitor
import com.netflix.java.refactor.refactor.fix.RefactorFix2
import com.netflix.java.refactor.refactor.fix.RefactorFixVisitor
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

        val mut = RefactorFix2(Cursor(listOf(a)), { cu: Tr.CompilationUnit ->
            cu.copy(imports = cu.imports + Tr.Import.build("java.util.Set"))
        })

        val a2 = RefactorFixVisitor(listOf(mut)).visit(a) as Tr.CompilationUnit
        val a3 = PositionCorrectingVisitor().visit(a2) as Tr.CompilationUnit

        assertEquals(2, a3.imports.size)
        
        val printed = PrintVisitor().visit(a3)
        println(printed)
    }
}
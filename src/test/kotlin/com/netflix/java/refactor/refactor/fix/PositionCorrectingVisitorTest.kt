package com.netflix.java.refactor.refactor.fix

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Source
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertTrue

class PositionCorrectingVisitorTest: AstTest(OracleJdkParser()) {
    
    @Test
    fun insertImport() {
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
        
        assertTrue(a3.imports[0].source is Source.Persisted)
        assertTrue(a3.imports[1].source is Source.Positioned)
    }
}
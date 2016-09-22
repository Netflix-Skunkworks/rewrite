package com.netflix.java.refactor.refactor.fix

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Source
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefactorFixTest : AstTest(OracleJdkParser()) {
    
    @Test
    fun lineNumbersAdjustedForRemoves() {
        val a = parse("""
            |import java.util.List;
            |public class A {
            |}
        """)
        
        val mut = RefactorFix2(Cursor(listOf(a)), { cu: Tr.CompilationUnit ->
            cu.copy(imports = emptyList())
        })
        
        val a2 = RefactorFixVisitor(listOf(mut)).visit(a) as Tr.CompilationUnit
        val a3 = PositionCorrectingVisitor().visit(a2) as Tr.CompilationUnit

        assertTrue(a2.imports.isEmpty())

        val classDeclSource = a3.classDecls[0].source
        assertTrue(classDeclSource is Source.Positioned)
        assertEquals(0, (classDeclSource as Source.Positioned).pos.start)
    }
    
    @Test
    fun lineNumbersAdjustedForAdds() {
        val a = parse("""
            |public class A {
            |}
        """)

        val mut = RefactorFix2(Cursor(listOf(a)), { cu: Tr.CompilationUnit ->
//            cu.copy(imports = Tr.Import())
            cu
        })

        val a2 = RefactorFixVisitor(listOf(mut)).visit(a) as Tr.CompilationUnit
        val a3 = PositionCorrectingVisitor().visit(a2) as Tr.CompilationUnit

        assertTrue(a2.imports.isEmpty())

        val classDeclSource = a3.classDecls[0].source
        assertTrue(classDeclSource is Source.Positioned)
        assertEquals(0, (classDeclSource as Source.Positioned).pos.start)
    }
}
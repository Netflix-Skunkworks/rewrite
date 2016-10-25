package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class EnumTest(parser: Parser): AstTest(parser) {

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

        assertTrue(a.typeDecls[0] is Tr.EnumClass)
        assertEquals("ONE(1)", (a.typeDecls[0] as Tr.EnumClass).values()[0].print())
    }

    @Test
    fun enumWithoutParameters() {
        val aSrc = "public enum A { ONE, TWO }"
        assertEquals(aSrc, parse(aSrc).typeDecls[0].print())
    }

    @Test
    fun enumWithEmptyParameters() {
        val aSrc = "public enum A { ONE(), TWO() }"
        assertEquals(aSrc, parse(aSrc).typeDecls[0].print())
    }
}
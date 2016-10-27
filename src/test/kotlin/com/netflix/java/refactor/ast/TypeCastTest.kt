package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class TypeCastTest(parser: Parser) : AstTest(parser) {

    @Test
    fun cast() {
        val a = parse("""
            public class A {
                Object o = (Class<String>) Class.forName("java.lang.String");
            }
        """)

        val typeCast = a.typeDecls[0].fields()[0].initializer as Tr.TypeCast
        assertEquals("""(Class<String>) Class.forName("java.lang.String")""",
                typeCast.printTrimmed())
    }
}
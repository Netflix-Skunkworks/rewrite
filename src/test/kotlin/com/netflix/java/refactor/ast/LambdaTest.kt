package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

abstract class LambdaTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            import java.util.function.Function;
            public class A {
                Function<String, String> func = (String s) -> "";
            }
        """)
    }

    val lambda by lazy { a.fields()[0].initializer as Tr.Lambda }

    @Test
    fun lambda() {
        assertEquals(1, lambda.params.size)
        assertTrue(lambda.body is Tr.Literal)
    }

    @Test
    fun format() {
        assertEquals("(String s) -> \"\"", lambda.printTrimmed())
    }
}
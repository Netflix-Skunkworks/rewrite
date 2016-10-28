package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals

abstract class LiteralTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun literalField() {
        val a = parse("""
            public class A {
                int n = 0;
            }
        """)

        val literal = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals(0, literal.value)
        assertEquals(Type.Tag.Int, literal.typeTag)
        assertEquals("0", literal.printTrimmed())
    }
    
    @Test
    fun transformString() {
        val a = parse("""
            public class A {
                String s = "foo ''";
            }
        """)

        val literal = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals("\"foo\"", literal.transformValue<String> { it.substringBefore(' ') })
    }

    @Test
    fun nullLiteral() {
        val a = parse("""
            public class A {
                String s = null;
            }
        """)

        assertEquals("null", a.fields()[0].vars[0].initializer?.printTrimmed())
    }

    @Test
    fun transformLong() {
        val a = parse("""
            public class A {
                Long l = 2L;
            }
        """)

        val literal = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals("4L", literal.transformValue<Long> { it * 2 })
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                Long l = 0l;
                Long m = 0L;
            }
        """)

        val (lower, upper) = a.fields(0..1).map { it.vars[0].initializer as Tr.Literal }

        assertEquals("0L", upper.printTrimmed())
        assertEquals("0l", lower.printTrimmed())
    }

    @Test
    fun formatEscapedString() {
        val a = parse("""
            public class A {
                String s = "\"";
            }
        """)

        val s = a.fields()[0].vars[0].initializer as Tr.Literal
        assertEquals("\"\\\"\"", s.printTrimmed())
    }
}
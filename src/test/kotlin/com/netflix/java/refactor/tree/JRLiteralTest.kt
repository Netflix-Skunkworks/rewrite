package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals

abstract class JRLiteralTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun literalField() {
        val a = parse("""
            public class A {
                int n = 0;
            }
        """)

        val literal = a.classDecls[0].fields[0].initializer as JRLiteral
        assertEquals(0, literal.value)
        assertEquals(JRType.Tag.Int, literal.typeTag)
        assertEquals("0", literal.source)
    }
    
    @Test
    fun transformString() {
        val a = parse("""
            public class A {
                String s = "foo ''";
            }
        """)

        val literal = a.classDecls[0].fields[0].initializer as JRLiteral
        assertEquals("\"foo\"", literal.transformValue<String> { it.substringBefore(' ') })
    }

    @Test
    fun transformLong() {
        val a = parse("""
            public class A {
                Long l = 2L;
            }
        """)

        val literal = a.classDecls[0].fields[0].initializer as JRLiteral
        assertEquals("4L", literal.transformValue<Long> { it * 2 })
    }
}
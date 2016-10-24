package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals

abstract class InstanceOfTest(parser: Parser): AstTest(parser) {

    val a by lazy {
        parse("""
            |public class A {
            |    Object o;
            |    public void test() {
            |        boolean b = o instanceof String;
            |    }
            |}
        """)
    }

    val variable by lazy { a.firstMethodStatement() as Tr.VariableDecl }
    val instanceof by lazy { variable.initializer as Tr.InstanceOf }

    @Test
    fun instanceOf() {
        assertEquals("java.lang.String", (instanceof.clazz as Tr.Ident).type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun format() {
        assertEquals("o instanceof String", instanceof.print())
    }
}
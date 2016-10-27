package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert
import org.junit.Test

abstract class ArrayTypeTest(parser: Parser): AstTest(parser) {

    @Test
    fun formatArrayReturnType() {
        val a = parse("""
            package a;
            public class A {
                public String[][] foo() { return null; }
            }
        """)

        val meth = a.typeDecls[0].methods()[0]
        Assert.assertEquals("public String[][] foo() { return null; }", meth.printTrimmed())
    }
}
package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals

abstract class InstanceOfTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun instanceOf() {
        val a = parse("""
            public class A {
                Object o;
                public void test() {
                    boolean b = o instanceof String;
                }
            }
        """)
        
        val variable = a.classDecls[0].methods[0].body.statements[0] as VariableDecl
        val instanceof = variable.initializer as InstanceOf
        assertEquals("java.lang.String", (instanceof.clazz as Ident).type.asClass()?.fullyQualifiedName)
    }
}
package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals

abstract class JRInstanceOfTest(parser: Parser): AstTest(parser) {
    
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
        
        val variable = a.classDecls[0].methods[0].body.statements[0] as JRVariableDecl
        val instanceof = variable.initializer as JRInstanceOf
        assertEquals("java.lang.String", (instanceof.clazz as JRIdent).type.asClass()?.fullyQualifiedName)
    }
}
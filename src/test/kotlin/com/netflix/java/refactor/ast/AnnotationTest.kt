package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class AnnotationTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun annotation() {
        val a = parse("""
            |@SuppressWarnings("ALL")
            |public class A {}
        """)
        
        val ann = a.classDecls[0].annotations[0]
        
        assertEquals("java.lang.SuppressWarnings", ann.type.asClass()?.fullyQualifiedName)
        assertEquals("ALL", ann.args.filterIsInstance<Tr.Literal>().firstOrNull()?.value)
    }
    
    @Test
    fun formatImplicitDefaultArgument() {
        val a = parse("""
            |@SuppressWarnings("ALL")
            |public class A {}
        """)
        
        val ann = a.classDecls[0].annotations[0]
        
        assertEquals("@SuppressWarnings(\"ALL\")", ann.print())
    }
}
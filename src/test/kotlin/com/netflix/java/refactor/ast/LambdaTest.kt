package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class LambdaTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun lambda() {
        val a = parse("""
            import java.util.function.Function;
            public class A {
                Function<String, String> func = (String s) -> "";
            }
        """)
        
        val lambda = a.classDecls[0].fields[0].initializer as Lambda
        assertEquals(1, lambda.params.size)
        assertTrue(lambda.body is Literal)
    }
}
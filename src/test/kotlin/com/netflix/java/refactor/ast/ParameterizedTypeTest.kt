package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class ParameterizedTypeTest(parser: Parser): AstTest(parser) {

    @Test
    fun format() {
        val a = parse("""
            import java.util.*;
            public class A {
                List< ?  extends  B > bs;
            }
        """, whichDependOn = "public class B {}")

        val typeParam = a.typeDecls[0].fields()[0].varType as Tr.ParameterizedType
        assertEquals("List< ?  extends  B >", typeParam.print())
    }
}
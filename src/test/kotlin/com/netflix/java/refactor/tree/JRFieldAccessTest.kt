package com.netflix.java.refactor.tree

import org.junit.Test
import kotlin.test.assertEquals

abstract class JRFieldAccessTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun fieldAccess() {
        val b = """
            public class B {
                public String field = "foo";
            }
        """
        
        val a = """
            public class A {
                B b = new B();
                String s = b.field;
            }
        """
        
        val acc = parse(a, whichDependsOn = b).classDecls[0].fields.first { it.name == "s" }.initializer as JRFieldAccess
        assertEquals("field", acc.fieldName)
        assertEquals("b", acc.target.source)
    }
}
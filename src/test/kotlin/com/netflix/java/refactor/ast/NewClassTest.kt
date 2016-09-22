package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import kotlin.test.assertEquals

abstract class NewClassTest(parser: Parser): AstTest(parser) {
    val a = """
        package a;
        public class A {
           public static class B { }
        }
    """
    
    @Test
    fun anonymousInnerClass() {
        val c = """
            import a.*;
            public class C {
                A.B anonB = new A.B() {};
            }
        """
        
        val b = parse(c, whichDependsOn = a).classDecls[0].fields[0]
        assertEquals("a.A.B", b.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun concreteInnerClass() {
        val c = """
            import a.*;
            public class C {
                A.B anonB = new A.B();
            }
        """

        val cu = parse(c, whichDependsOn = a)
        val b = cu.classDecls[0].fields[0]
        assertEquals("a.A.B", b.type.asClass()?.fullyQualifiedName)
        assertEquals("A.B", (b.initializer as Tr.NewClass).identifier.source.text(cu))
    }
    
    @Test
    fun concreteClassWithParams() {
        val a = parse("""
            import java.util.*;
            public class A {
                Object l = new ArrayList<String>(0);
            }
        """)

        val newClass = a.classDecls[0].fields[0].initializer as Tr.NewClass
        assertEquals(1, newClass.args.size)
    }
}
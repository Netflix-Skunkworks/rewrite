package com.netflix.java.refactor.search

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertTrue

abstract class FindFieldsTest(parser: Parser) : AstTest(parser) {
    
    @Test
    fun findField() {
        val a = parse("""
            import java.util.*;
            public class A {
               List list = new ArrayList<>();
            }
        """)

        val field = a.findFields(List::class.java).first()
        assertEquals("list", field.name)
        assertEquals("java.util.List", field.type)
    }
    
    @Test
    fun findPrivateNonInheritedField() {
        val a = parse("""
            import java.util.List;
            public class A {
               private List list;
            }
        """)

        assertEquals("list", a.findFields(List::class.java).firstOrNull()?.name)
    }
    
    @Test
    fun findInheritedField() {
        val a = """
            import java.util.*;
            public class A {
               List list;
               private Set set;
            }
        """
        
        val b = "public class B extends A { }"

        assertTrue(parse(b, a).findFields(List::class.java).isEmpty())

        assertEquals("list", parse(b, a).findFieldsIncludingInherited(List::class.java).firstOrNull()?.name)
        assertTrue(parse(b, a).findFieldsIncludingInherited(Set::class.java).isEmpty())
    }

    // FIXME this is intended to test the case where there is something that satisfies cu.defs.find { it.type == null }, but
    // doesn't currently
    @Test
    fun unresolvableTypeSymbol() {
        val b = parse("""
            import java.util.List;
            public class <T extends A> B<T> {
                List unresolvable;
            }
		""")

        b.findFields(List::class.java)
    }
}

class OracleJdkFindFieldsTest: FindFieldsTest(OracleJdkParser())
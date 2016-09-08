package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test

abstract class ChangeFieldTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun changeFieldType() {
        val a = parse("""
            |import java.util.List;
            |public class A {
            |   List collection;
            |}
        """)
        
        a.refactor()
                .findFieldsOfType(List::class.java)
                    .changeType(Collection::class.java)
                    .done()
                .fix()
        a.refactor()
                .removeImport(List::class.java)
                .fix()
        
        assertRefactored(a, """
            |import java.util.Collection;
            |public class A {
            |   Collection collection;
            |}
        """)
    }
    
    @Test
    fun changeFieldName() {
        val a = parse("""
            |import java.util.List;
            |public class A {
            |   List collection = null;
            |}
        """)

        a.refactor()
                .findFieldsOfType(List::class.java)
                    .changeName("list")
                    .done()
                .fix()

        assertRefactored(a, """
            |import java.util.List;
            |public class A {
            |   List list = null;
            |}
        """)
    }
    
    @Test
    fun deleteField() {
        val a = parse("""
            |import java.util.List;
            |public class A {
            |   List collection = null;
            |}
        """)

        a.refactor()
                .findFieldsOfType(List::class.java)
                .delete()
                .fix()

        assertRefactored(a, """
            |import java.util.List;
            |public class A {
            |}
        """)
    }
}

class OracleJdkChangeFieldTest: ChangeFieldTest(OracleJdkParser())
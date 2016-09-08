package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test

abstract class AddFieldTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun addField() {
        val a = parse("""
            |class A {
            |}
        """)

        a.refactor().addField(List::class.java, "list", "new ArrayList<>()").fix()

        assertRefactored(a, """
            |import java.util.List;
            |class A {
            |   List list = new ArrayList<>();
            |}
        """)
    }
}

class OracleJdkAddFieldTest: AddFieldTest(OracleJdkParser())
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class AddFieldTest(p: Parser): Parser by p {
    
//    @Test
//    fun addField() {
//        val a = parse("""
//            |class A {
//            |}
//        """)
//
//        a.typeDecls[0].refactor().addField(List::class.java, "list", "new ArrayList<>()").fix()
//
//        assertRefactored(a, """
//            |import java.util.List;
//            |class A {
//            |   List list = new ArrayList<>();
//            |}
//        """)
//    }
}

class OracleJdkAddFieldTest: AddFieldTest(OracleJdkParser())
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class ChangeLiteralArgumentTest(p: Parser): Parser by p {

    private val b: String = """
        |class B {
        |   public void singleArg(String s) {}
        |}
    """

    @Test
    fun changeStringLiteralArgument() {
        val a = """
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().singleArg("foo (%s)" + s + 0L);
            |   }
            |}
        """

        val cu = parse(a, b)
        val fixed = cu.refactor() {
            cu.findMethodCalls("B singleArg(String)").forEach {
                changeLiterals(it.args.args[0]) { s -> s?.toString()?.replace("%s", "{}") ?: s }
            }
        }.fix()

        assertRefactored(fixed, """
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().singleArg("foo ({})" + s + 0L);
            |   }
            |}
        """)
    }

    @Test
    fun changeStringLiteralArgumentWithEscapableCharacters() {
        val a = """
            |package a;
            |public class A {
            |    public void foo(String s) {}
            |}
        """

        val b = """
            |import a.*;
            |public class B {
            |    A a;
            |    public void test() {
            |        a.foo("mystring '%s'");
            |    }
            |}
        """

        val cu = parse(b, a)
        val fixed = cu.refactor {
            cu.findMethodCalls("a.A foo(..)").forEach {
                changeLiterals(it.args.args[0]) { s -> s?.toString()?.replace("%s", "{}") ?: s }
            }
        }.fix()

        assertRefactored(fixed, """
            |import a.*;
            |public class B {
            |    A a;
            |    public void test() {
            |        a.foo("mystring '{}'");
            |    }
            |}
        """)
    }
}

class OracleChangeLiteralArgumentTest: ChangeLiteralArgumentTest(OracleJdkParser())
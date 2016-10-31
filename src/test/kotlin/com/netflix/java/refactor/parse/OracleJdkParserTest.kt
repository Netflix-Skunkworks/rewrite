package com.netflix.java.refactor.parse

import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import java.net.URL

class OracleJdkParserTest : AstTest(OracleJdkParser()) {

    /**
     * Often type attribution can succeed in spite of symbol entering failures, but there are edge cases
     * where it does not. Therefore, attribution after symbol entering failures is always a BEST EFFORT only.
     */
    @Test
    fun typeAttributionDoesNotCauseRuntimeExceptionsWhenSymbolEnteringFails() {
        parse("""
            |import java.util.function.Consumer;
            |public class A {
            |    public void fail(){
            |        Consumer<String> c = s -> {
            |            Function<String, OOPS> f = s2 -> null;
            |        };
            |    }
            |}
        """)
    }
    
    @Test
    fun parserIsAbleToIdentifyTypesFromExternalDependencies() {
        val testngDownloaded = URL("http://repo1.maven.org/maven2/org/testng/testng/6.9.9/testng-6.9.9.jar").openStream().readBytes()
        val testng = temp.newFile("testng-6.9.9.jar")
        testng.outputStream().use { it.write(testngDownloaded) }
        
        parser = OracleJdkParser(listOf(testng.toPath()))

        val a = parse("""
            |package a;
            |import org.testng.annotations.*;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """)
        
        a.refactor()
                .changeType("org.testng.annotations.Test", "org.junit.Test")
                .fix()

        // FIXME the import remains because RemoveImport can't tell whether you intend to use org.junit.Test or
        // org.testng.annotations.Test
        assertRefactored(a, """
            |package a;
            |import org.junit.Test;
            |import org.testng.annotations.Test;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """)
    }
}
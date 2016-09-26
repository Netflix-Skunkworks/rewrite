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
    
    // FIXME how to do this?
//    @Ignore
//    @Test
//    fun parserIsAbleToLoadExternalDependenciesFromInMemoryFileSystems() {
//        val fs = MemoryFileSystemBuilder.newEmpty().build("virtual")
//        fs.use { fs ->
//            val a = fs.getPath("A.java")
//            Files.write(a, """
//                |package a;
//                |import org.testng.annotations.*;
//                |public class A {
//                |   @Test
//                |   public void test() {}
//                |}
//            """.trimMargin().toByteArray())
//
//            val zin = ZipInputStream(URL("http://repo1.maven.org/maven2/org/testng/testng/6.9.9/testng-6.9.9.jar").openStream())
//            var entry = zin.nextEntry
//            while (entry != null) {
//                val path = fs.getPath("testng-6.9.9/${entry.name}")
//                if(!Files.exists(path.parent))
//                    Files.createDirectories(path.parent)
//                if(!entry.isDirectory)
//                    Files.copy(zin, path)
//            }
//            
////            val classes = SourceSet(listOf(a), listOf(fs.getPath("testng-6.9.9"))).scanForClasses { it.hasType("org.testng.annotations.Test") }
////            assertEquals(listOf("a.A"), classes)
//        }
//    }
}
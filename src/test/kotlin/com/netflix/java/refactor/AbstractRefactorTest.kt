package com.netflix.java.refactor

import com.netflix.java.refactor.ast.AstParser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class AbstractRefactorTest {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    fun refactor(target: File, vararg otherFiles: File) =
        refactor(target, otherFiles.toList(), null)
    
    fun refactor(target: File, otherFiles: Iterable<File>, classpath: Iterable<File>? = null): JavaSource {
        val parser = AstParser(classpath)
        val allFiles = otherFiles.plus(target)
        val cu = parser.parseFiles(allFiles.toList()).last()
        return JavaSource(CompilationUnit(cu, parser))
    }
    
    fun java(sourceStr: String): File {
        val source = temp.newFile(fullyQualifiedName(sourceStr.trimMargin()) + ".java")
        source.writeText(sourceStr.trimMargin())
        return source
    }
    
    fun assertRefactored(file: File, refactored: String) {
        assertEquals(refactored.trimMargin(), file.readText())
    }
}
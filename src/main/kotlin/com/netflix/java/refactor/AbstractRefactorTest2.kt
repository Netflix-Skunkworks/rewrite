package com.netflix.java.refactor

import com.netflix.java.refactor.ast.AstParser
import com.netflix.java.refactor.tree.JRCompilationUnit
import com.netflix.java.refactor.tree.oraclejdk.toAst
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.regex.Pattern

abstract class AbstractRefactorTest2 {
    @JvmField @Rule
    val temp = TemporaryFolder()

    fun parseJava(target: File, vararg otherFiles: File): JavaSource2 =
            parseJava(target, otherFiles.toList())

    fun parseJava(target: File, otherFiles: Iterable<File>): JavaSource2 =
            JavaSource2(parseJavaToCompilationUnit(target, otherFiles))

    fun parseJavaToCompilationUnit(target: File, vararg otherFiles: File): JRCompilationUnit =
            parseJavaToCompilationUnit(target, otherFiles.toList())

    fun parseJavaToCompilationUnit(target: File, otherFiles: Iterable<File> = emptyList()): JRCompilationUnit {
        val parser = AstParser(null)
        val allFiles = otherFiles.plus(target)
        return parser.parseFiles(allFiles.map { it.toPath() }).last().toAst()
    }

    fun java(sourceStr: String): File {
        val source = temp.newFile(fullyQualifiedName(sourceStr.trimMargin())!!.substringAfterLast(".") + ".java")
        source.writeText(sourceStr.trimMargin())
        return source
    }

    fun assertRefactored(file: File, refactored: String) {
        Assert.assertEquals(refactored.trimMargin(), file.readText())
    }

    fun fullyQualifiedName(sourceStr: String): String? {
        val pkgMatcher = Pattern.compile("\\s*package\\s+([\\w\\.]+)").matcher(sourceStr)
        val pkg = if (pkgMatcher.find()) pkgMatcher.group(1) + "." else ""

        val classMatcher = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)").matcher(sourceStr)
        return if (classMatcher.find()) pkg + classMatcher.group(3) else null
    }
}
package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.Tr
import java.nio.file.Path

abstract class Parser(classpath: List<Path>?) {
    val filteredClasspath: List<Path>? = classpath?.filter {
        val fn = it.fileName.toString()
        fn.endsWith(".jar") && !fn.endsWith("-javadoc.jar") && !fn.endsWith("-sources.jar")
    }

    abstract fun parse(sourceFiles: List<Path>, sourceFactory: (Path) -> RawSourceCode): List<Tr.CompilationUnit>
    
    protected fun filterSourceFiles(sourceFiles: List<Path>) =
        sourceFiles.filter { it.fileName.toString().endsWith(".java") }.toList()
}
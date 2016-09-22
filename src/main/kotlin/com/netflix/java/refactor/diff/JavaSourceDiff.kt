package com.netflix.java.refactor.diff

import com.netflix.java.refactor.ast.Source
import com.netflix.java.refactor.ast.Tr

class JavaSourceDiff(private val cu: Tr.CompilationUnit,
                     private val source: Source, 
                     val path: String) {
    private val before = source.text(cu)
    fun gitStylePatch() = InMemoryDiffEntry(path, before, source.text(cu)).diff  
}
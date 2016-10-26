package com.netflix.java.refactor.diff

import com.netflix.java.refactor.ast.Tr

class JavaSourceDiff(private val cu: Tr.CompilationUnit) {
    private val before = cu.printTrimmed()
    fun gitStylePatch() = InMemoryDiffEntry(cu.source.path, before, cu.printTrimmed()).diff
}
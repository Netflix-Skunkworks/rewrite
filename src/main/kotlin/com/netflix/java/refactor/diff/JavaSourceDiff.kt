package com.netflix.java.refactor.diff

import com.netflix.java.refactor.ast.Tr

class JavaSourceDiff(private val cu: Tr.CompilationUnit) {
    private val before = cu.print()
    fun gitStylePatch() = InMemoryDiffEntry(cu.source.path, before, cu.print()).diff  
}
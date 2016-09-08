package com.netflix.java.refactor.diff

import com.netflix.java.refactor.parse.Source

class JavaSourceDiff(private val source: Source) {
    private val before = source.text
    fun gitStylePatch() = InMemoryDiffEntry(source.path, before, source.text).diff  
}
package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.parse.Source

data class RefactorFix(val position: IntRange,
                       val changes: String?,
                       val source: Source) {
    val lineNumber: Int by lazy {
        source.text.substring(0, position.start).count { it == '\n' } + 1
    }
}
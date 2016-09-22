package com.netflix.java.refactor.refactor.fix

//data class RefactorFix(val position: IntRange,
//                       val changes: String?,
//                       val source: RawSourceCode) {
//    val lineNumber: Int by lazy {
//        source.text.substring(0, position.start).count { it == '\n' } + 1
//    }
//}

// FIXME delete me!
@Deprecated("Replaced with RefactorFix2")
class RefactorFix
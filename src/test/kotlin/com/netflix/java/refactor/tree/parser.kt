package com.netflix.java.refactor.tree

import java.util.regex.Pattern

abstract class AstTest(val parser: Parser) {
    fun parse(source: String, vararg whichDependsOn: String) = parser(source, whichDependsOn.toList())
}

typealias Parser = (source: String, whichDependsOn: List<String>) -> JRCompilationUnit

fun simpleName(sourceStr: String): String? {
    val pkgMatcher = Pattern.compile("\\s*package\\s+([\\w\\.]+)").matcher(sourceStr)
    val pkg = if (pkgMatcher.find()) pkgMatcher.group(1) + "." else ""

    val classMatcher = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)").matcher(sourceStr)
    return if (classMatcher.find()) classMatcher.group(3) else null
}
package com.netflix.java.refactor.tree

import java.util.regex.Pattern

class JRLiteral(val typeTag: JRType.Tag,
                val value: Any,
                override val pos: IntRange,
                override val source: String): JRExpression {
    override fun <R> accept(v: JRTreeVisitor<R>): R = v.visitLiteral(this)

    /**
     * Primitive values sometimes contain a prefix and suffix that hold the special characters, 
     * e.g. the "" around String, the L at the end of a long, etc.
     */
    fun <T> transformValue(transform: (T) -> Any): String {
        val valueMatcher = "(.*)${Pattern.quote(value.toString())}(.*)".toRegex().find(source.replace("\\", ""))
        return when(valueMatcher) {
            is MatchResult -> {
                val (prefix, suffix) = valueMatcher.groupValues.drop(1)
                @Suppress("UNCHECKED_CAST")
                return "$prefix${transform(value as T)}$suffix"
            }
            else -> {
                throw IllegalStateException("Encountered a literal `$source` that could not be transformed")
            }
        }
    }
}
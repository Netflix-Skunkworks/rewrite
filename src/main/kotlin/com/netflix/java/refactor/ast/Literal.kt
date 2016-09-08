package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Source
import java.util.regex.Pattern

class Literal(val typeTag: Type.Tag,
              val value: Any,
              override val type: Type?,
              override val pos: IntRange): Expression {
    override fun <R> accept(v: AstVisitor<R>): R = v.visitLiteral(this)

    /**
     * Primitive values sometimes contain a prefix and suffix that hold the special characters, 
     * e.g. the "" around String, the L at the end of a long, etc.
     */
    fun <T> transformValue(source: Source, transform: (T) -> Any): String {
        val valueMatcher = "(.*)${Pattern.quote(value.toString())}(.*)".toRegex().find(source.snippet(this).replace("\\", ""))
        return when(valueMatcher) {
            is MatchResult -> {
                val (prefix, suffix) = valueMatcher.groupValues.drop(1)
                @Suppress("UNCHECKED_CAST")
                return "$prefix${transform(value as T)}$suffix"
            }
            else -> {
                throw IllegalStateException("Encountered a literal `$this` that could not be transformed")
            }
        }
    }
}
package com.netflix.java.refactor.ast

import java.io.Serializable

interface Tree : Serializable {
    fun <R> accept(v: AstVisitor<R>): R
    val pos: IntRange
}
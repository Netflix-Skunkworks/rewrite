package com.netflix.java.refactor.tree

import java.io.Serializable

interface JRTree: Serializable {
    fun <R> accept(v: JRTreeVisitor<R>): R
    val pos: IntRange
}
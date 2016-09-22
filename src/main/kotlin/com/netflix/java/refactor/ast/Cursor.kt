package com.netflix.java.refactor.ast

data class Cursor(val path: List<Tree>) {
    fun plus(t: Tree) = copy(path + t)
    
    companion object {
        val Empty = Cursor(emptyList())
    }
}

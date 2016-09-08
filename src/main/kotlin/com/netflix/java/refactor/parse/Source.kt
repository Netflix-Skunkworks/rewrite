package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.Tree

interface Source {
    val text: String
    val path: String
    fun fix(newSource: String)
    
    fun snippet(tree: Tree) = text.substring(tree.pos)
}
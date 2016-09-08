package com.netflix.java.refactor.ast

interface Expression : Tree {
    val type: Type?
}
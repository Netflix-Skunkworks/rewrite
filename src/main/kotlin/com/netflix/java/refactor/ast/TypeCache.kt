package com.netflix.java.refactor.ast

import java.util.*

class TypeCache {
    val packagePool = HashMap<String, Type.Package>()
    val classPool = HashMap<String, Type.Class>()
}
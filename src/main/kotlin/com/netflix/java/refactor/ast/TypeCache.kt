package com.netflix.java.refactor.ast

import java.util.*

data class TypeCache private constructor(val key: UUID) {
    val packagePool = HashMap<String, Type.Package>()
    val classPool = HashMap<String, Type.Class>()

    companion object {
        private val caches = WeakHashMap<UUID, TypeCache>()

        fun of(key: UUID): TypeCache = caches.getOrPut(key) { TypeCache(key) }

        fun new(): TypeCache {
            val uid = UUID.randomUUID()
            val cache = TypeCache(uid)
            caches.put(uid, cache)
            return cache
        }
    }

    fun reset() {
        packagePool.clear()
        classPool.clear()
    }
}
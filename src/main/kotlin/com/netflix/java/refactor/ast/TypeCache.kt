package com.netflix.java.refactor.ast

import com.google.common.io.BaseEncoding
import java.util.*

data class TypeCache private constructor(val key: String) {
    val packagePool = HashMap<String, Type.Package>()
    val classPool = HashMap<String, Type.Class>()

    companion object {
        private val caches = WeakHashMap<String, TypeCache>()
        val random = Random()

        fun of(key: String): TypeCache = caches.getOrPut(key) { TypeCache(key) }

        fun new(): TypeCache {
            val buffer = ByteArray(5)
            random.nextBytes(buffer)
            val uid = BaseEncoding.base64Url().omitPadding().encode(buffer)
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
package com.netflix.java.refactor.ast

import org.junit.Test
import kotlin.test.assertEquals

class TypeUtilsTest {
    
    @Test
    fun packageOwner() {
        assertEquals("com.foo", com.netflix.java.refactor.ast.packageOwner("com.foo.Foo"))
        assertEquals("com.foo", com.netflix.java.refactor.ast.packageOwner("com.foo.Foo.Bar"))
    }

    @Test
    fun className() {
        assertEquals("Foo", com.netflix.java.refactor.ast.className("com.foo.Foo"))
        assertEquals("Foo.Bar", com.netflix.java.refactor.ast.className("com.foo.Foo.Bar"))
    }
}
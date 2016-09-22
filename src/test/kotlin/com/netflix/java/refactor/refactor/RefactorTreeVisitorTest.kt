package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.refactor.fix.RefactorTreeVisitor
import org.junit.Test
import kotlin.test.assertEquals

class RefactorTreeVisitorTest {
    
    @Test
    fun packageOwner() {
        assertEquals("com.foo", RefactorTreeVisitor.packageOwner("com.foo.Foo"))
        assertEquals("com.foo", RefactorTreeVisitor.packageOwner("com.foo.Foo.Bar"))
    }

    @Test
    fun className() {
        assertEquals("Foo", RefactorTreeVisitor.className("com.foo.Foo"))
        assertEquals("Foo.Bar", RefactorTreeVisitor.className("com.foo.Foo.Bar"))
    }
}
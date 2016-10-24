package com.netflix.java.refactor.ast

import org.junit.Assert.assertEquals
import org.junit.Test

class TreeBuilderTest {
    val cache = TypeCache()

    @Test
    fun buildFullyQualifiedClassName() {
        val name = TreeBuilder.buildName(cache, "java.util.List", Formatting.Reified.Empty) as Tr.FieldAccess

        assertEquals("java.util.List", name.print())
        assertEquals("List", name.fieldName.name)
    }

    @Test
    fun buildFullyQualifiedInnerClassName() {
        val name = TreeBuilder.buildName(cache, "a.Outer.Inner", Formatting.Reified.Empty) as Tr.FieldAccess

        assertEquals("a.Outer.Inner", name.print())
        assertEquals("Inner", name.fieldName.name)
        assertEquals("a.Outer.Inner", name.type.asClass()?.fullyQualifiedName)

        val outer = name.target as Tr.FieldAccess
        assertEquals("Outer", outer.fieldName.name)
        assertEquals("a.Outer", outer.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun buildStaticImport() {
        val name = TreeBuilder.buildName(cache, "a.A.*", Formatting.Reified.Empty) as Tr.FieldAccess

        assertEquals("a.A.*", name.print())
        assertEquals("*", name.fieldName.name)
    }
}
package com.netflix.java.refactor.tree

import java.io.Serializable

sealed class JRType(): Serializable {
    abstract class JRTypeWithOwner: JRType() {
        abstract val owner: JRType?

        fun ownedByType(clazz: String): Boolean =
            if (this is JRType.Class && fullyQualifiedName == clazz)
                true
            else if(owner is JRTypeWithOwner) 
                (owner as JRTypeWithOwner).ownedByType(clazz) 
            else false
    }
    
    data class Package(val fullName: String, override val owner: JRType?): JRTypeWithOwner()
    
    data class Class(val fullyQualifiedName: String, override val owner: JRType?): JRTypeWithOwner()
    
    data class Method(val returnType: JRType?, val paramTypes: List<JRType>): JRType()
   
    data class GenericTypeVariable(val name: String, val bound: Class?): JRType()
    
    data class Array(val elemType: JRType): JRType()
    
    data class Primitive(val typeTag: Tag): JRType()

    enum class Tag {
        Boolean,
        Byte,
        Char,
        Double,
        Float,
        Int,
        Long,
        Short,
        Void,
        Class,
        None,
        Wildcard
    }
}

fun JRType?.asClass() = this as JRType.Class
fun JRType?.asGeneric() = this as JRType.GenericTypeVariable

fun JRType?.isArrayOfType(qualifiedNameOrTypeVar: String): Boolean = when(this) {
    is JRType.Array -> when(this.elemType) {
        is JRType.Class -> this.elemType.fullyQualifiedName == qualifiedNameOrTypeVar
        is JRType.GenericTypeVariable -> this.elemType.name == qualifiedNameOrTypeVar
        else -> false
    }
    else -> false
}
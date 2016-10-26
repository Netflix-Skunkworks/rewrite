package com.netflix.java.refactor.refactor.fix

import com.netflix.java.refactor.ast.AstVisitor
import com.netflix.java.refactor.ast.Tree
import com.netflix.java.refactor.ast.Type

// FIXME delete me!
@Deprecated("Use ")
abstract class RefactorTreeVisitor: AstVisitor<List<RefactorFix>>({ emptyList() }) {
    fun Tree.replace(changes: String) = RefactorFix() //RefactorFix(pos, changes, source)

    fun Iterable<Tree>.replace(changes: String): RefactorFix {
        assert(this.iterator().hasNext())
        return RefactorFix()
//        return RefactorFix(first().pos.start..last().pos.endInclusive, changes, source)
    }

    fun replace(range: IntRange, changes: String) = RefactorFix() //RefactorFix(range, changes, source)

    fun Tree.insertAt(changes: String) = RefactorFix()
//            RefactorFix(pos.endInclusive..pos.endInclusive, changes, source)

    fun insertAt(pos: Int, changes: String): RefactorFix = RefactorFix()
//            RefactorFix(pos..pos, changes, source)

    fun Tree.insertBefore(changes: String) = RefactorFix()
//            RefactorFix(pos.start..pos.start, changes, source)

    fun Type?.matches(fullyQualifiedClassName: String?) = when(this) {
        is Type.Class -> matches(this.fullyQualifiedName, fullyQualifiedClassName)
        else -> false
    }

    fun Tree?.matches(fullyQualifiedClassName: String?) = when(this) {
        is Tree -> matches(this.printTrimmed(), fullyQualifiedClassName)
        else -> false
    }

    private fun matches(javacTypeSerialization: String, fullyQualifiedClassName: String?) =
            if(fullyQualifiedClassName is String)
                javacTypeSerialization == "${packageOwner(fullyQualifiedClassName)}.${className(fullyQualifiedClassName).replace(".", "\$")}"
            else false
    
    fun Tree.delete(): RefactorFix {
//        var start = pos.start
//        var end = pos.endInclusive
//        if(source.text.length > end && source.text[end+1] == '\n') {
//            // delete the newline and any leading whitespace too
//            end++
//
//            while(start > 0 && source.text[start-1].isWhitespace() && source.text[start-1] != '\n') {
//                start--
//            }
//        }
//        return RefactorFix(start..end, null, source)
        return RefactorFix()
    }

    companion object {
        fun packageOwner(fullyQualifiedClassName: String) =
                fullyQualifiedClassName.split('.').dropLastWhile { it[0].isUpperCase() }.joinToString(".")

        fun className(fullyQualifiedClassName: String) =
                fullyQualifiedClassName.split('.').dropWhile { it[0].isLowerCase() }.joinToString(".")
    }
}
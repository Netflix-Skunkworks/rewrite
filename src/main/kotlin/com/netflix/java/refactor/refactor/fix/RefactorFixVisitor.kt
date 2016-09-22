package com.netflix.java.refactor.refactor.fix

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tree
import com.netflix.java.refactor.refactor.TransformationVisitor

class RefactorFixVisitor(val mutations: Iterable<RefactorFix2<*>>): TransformationVisitor() {
    
    override fun <T : Tree> T.mutate(cursor: Cursor, childCopy: T.() -> T): T =
        (listOf(childCopy) + mutations
                .filterIsInstance<RefactorFix2<T>>()
                .filter { it.cursor == cursor }
                .map { it.mutation }).fold(this) { mutated, mut -> mut(mutated) }

    
}
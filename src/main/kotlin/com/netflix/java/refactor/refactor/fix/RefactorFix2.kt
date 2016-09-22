package com.netflix.java.refactor.refactor.fix

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tree

data class RefactorFix2<T: Tree>(val cursor: Cursor, val mutation: (T) -> T)
/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ArrayAccessTest : JavaParser() {
    private val a: J.CompilationUnit by lazy {
        parse("""
            public class a {
                int n[] = new int[] { 0 };
                public void test() {
                    int m = n[0];
                }
            }
        """)
    }

    private val variable by lazy { a.firstMethodStatement() as J.VariableDecls }
    private val arrAccess by lazy { variable.vars[0].initializer as J.ArrayAccess }

    @Test
    fun arrayAccess() {
        assertTrue(arrAccess.indexed is J.Ident)
        assertTrue(arrAccess.dimension.index is J.Literal)
    }

    @Test
    fun format() {
        assertEquals("n[0]", arrAccess.printTrimmed())
    }
}
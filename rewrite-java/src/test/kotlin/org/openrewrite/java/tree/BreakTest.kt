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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class BreakTest : JavaParser() {

    @Test
    fun breakFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) break;
                }
            }
        """)

        val whileLoop = a.firstMethodStatement() as J.WhileLoop
        assertTrue(whileLoop.body is J.Break)
        assertNull((whileLoop.body as J.Break).label)
    }

    @Test
    fun breakFromLabeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as J.Label).statement as J.WhileLoop
        assertTrue(whileLoop.body is J.Break)
        assertEquals("labeled", (whileLoop.body as J.Break).label?.simpleName)
    }

    @Test
    fun formatLabeledBreak() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled : while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as J.Label).statement as J.WhileLoop
        assertEquals("break labeled", whileLoop.body.printTrimmed())
    }
}
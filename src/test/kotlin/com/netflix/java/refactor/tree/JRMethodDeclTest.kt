package com.netflix.java.refactor.tree

import com.sun.tools.javac.tree.JCTree
import org.junit.Test
import kotlin.test.assertEquals

abstract class JRMethodDeclTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun methodDecl() {
        val p = "public class P {}"
        
        val a = """
            public class A {
                public <P> P foo(P p, String s, String... args) {
                    return p;
                }
            }
        """
        
        val meth = parse(a, whichDependsOn = p).classDecls[0].methods[0]
        assertEquals("foo", meth.name)
        assertEquals(3, meth.params.size)
        assertEquals(1, meth.body.statements.size)
        assertEquals("P", ((meth.returnTypeExpr as JRIdent).type as JRType.GenericTypeVariable).name)
    }
}
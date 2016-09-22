package com.netflix.java.refactor.refactor.fix

import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Source
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Tree
import com.netflix.java.refactor.refactor.TransformationVisitor

class PositionCorrectingVisitor: TransformationVisitor() {
    var position: Int = 0
    
    override fun <T : Tree> T.mutate(cursor: Cursor, childCopy: T.() -> T): T {
        val newPos = position .. position + source.length()
        position = newPos.start

        val tr = childCopy(this) as Tr
        
        val src = source
        val newSource = when(src) {
            is Source.Insertion -> Source.Positioned(newPos, src.text(cu), "", "")
            is Source.Persisted -> 
                if(src.pos == newPos) src 
                    
                    // FIXME src.text(cu) is not correct here, as its nested contents may have changed
                else Source.Positioned(newPos, src.text(cu), src.prefix, src.suffix) // this node has been shifted from its original position
            is Source.Positioned -> Source.Positioned(newPos, src.text(cu), src.prefix, src.suffix)
            is Source.None -> Source.None
            is Source.All -> Source.All
        }
        
        @Suppress("UNCHECKED_CAST")
        return when(tr) {
            is Tr.ArrayAccess -> tr.copy(source = newSource)
            is Tr.Assign -> tr.copy(source = newSource)
            is Tr.AssignOp -> tr.copy(source = newSource)
            is Tr.Binary -> tr.copy(source = newSource)
            is Tr.Block -> tr.copy(source = newSource)
            is Tr.Break -> tr.copy(source = newSource)
            is Tr.Case -> tr.copy(source = newSource)
            is Tr.Catch -> tr.copy(source = newSource)
            is Tr.ClassDecl -> tr.copy(source = newSource)
            is Tr.CompilationUnit -> tr.copy()
            is Tr.Continue -> tr.copy(source = newSource)
            is Tr.DoWhileLoop -> tr.copy(source = newSource)
            is Tr.Empty -> Tr.Empty
            is Tr.FieldAccess -> tr.copy(source = newSource)
            is Tr.ForEachLoop -> tr.copy(source = newSource)
            is Tr.ForLoop -> tr.copy(source = newSource)
            is Tr.Ident -> tr.copy(source = newSource)
            is Tr.If -> tr.copy(source = newSource)
            is Tr.Import -> {
                tr.copy(source = newSource)
            }
            is Tr.InstanceOf -> tr.copy(source = newSource)
            is Tr.Label -> tr.copy(source = newSource)
            is Tr.Lambda -> tr.copy(source = newSource)
            is Tr.Literal -> tr.copy(source = newSource)
            is Tr.MethodDecl -> tr.copy(source = newSource)
            is Tr.MethodInvocation -> tr.copy(source = newSource)
            is Tr.NewArray -> tr.copy(source = newSource)
            is Tr.NewClass -> tr.copy(source = newSource)
            is Tr.Parentheses -> tr.copy(source = newSource)
            is Tr.Primitive -> tr.copy(source = newSource)
            is Tr.Return -> tr.copy(source = newSource)
            is Tr.Switch -> tr.copy(source = newSource)
            is Tr.Synchronized -> tr.copy(source = newSource)
            is Tr.Ternary -> tr.copy(source = newSource)
            is Tr.Throw -> tr.copy(source = newSource)
            is Tr.Try -> tr.copy(source = newSource)
            is Tr.Unary -> tr.copy(source = newSource)
            is Tr.VariableDecl -> tr.copy(source = newSource)
            is Tr.WhileLoop -> tr.copy(source = newSource)
            is Tr.Token -> tr.copy(source = newSource)
        } as T
    }
}
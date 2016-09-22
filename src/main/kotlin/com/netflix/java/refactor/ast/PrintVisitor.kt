package com.netflix.java.refactor.ast

class PrintVisitor: AstVisitor<String>("") {
    override fun reduce(r1: String, r2: String): String = r1 + r2

    var prevPos: Int = 0
    
    override fun visit(tree: Tree?, cursor: Cursor): String {
        if(tree is Tr.CompilationUnit)
            cu = tree
        
        return if (tree is Tree) {
            val source = tree.source
            val endOfPriorWhitespace = when (source) {
                is Source.Positioned -> source.pos.start
                is Source.Persisted -> source.pos.start
                else -> prevPos
            }

            val sourceWithLeadingWhitespace = cu.rawSource.text.substring(prevPos, endOfPriorWhitespace) + super.visit(tree, cursor)

            // move prevPos to start of whitespace after this tree
            when (source) {
                is Source.Positioned -> prevPos = source.pos.endInclusive + 1
                is Source.Persisted -> prevPos = source.pos.endInclusive + 1
            }

            sourceWithLeadingWhitespace
        } else ""
    }
    
    override fun visitImport(import: Tr.Import, cursor: Cursor): String {
        return import.source.text(cu)
    }
}
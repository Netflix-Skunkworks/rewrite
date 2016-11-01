package com.netflix.java.refactor.ast.visitor

import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Tree

/**
 * Only formatting for nodes that we can ADD so far
 * is supported.
 */
class FormatVisitor: AstVisitor<Tree?>({ it }) {

    lateinit var cu: Tr.CompilationUnit

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): Tree? {
        this.cu = cu
        return super.visitCompilationUnit(cu)
    }

    override fun visitImport(import: Tr.Import): Tree? {
        if(import.formatting is Formatting.Infer) {
            // we are assuming throughout no common indentation of the whole file (including imports and class decls)
            if (cu.imports.size > 1) {
                var importPassed = false
                val firstSubsequentImport = cu.imports.find {
                    if(it == import) {
                        importPassed = true
                        false
                    }
                    else importPassed
                }

                if(firstSubsequentImport != null) {
                    import.formatting = firstSubsequentImport.formatting
                    firstSubsequentImport.formatting = Formatting.Reified("\n")
                }
                else {
                    // last import in the list
                    import.formatting = Formatting.Reified("\n")
                }
            } else if(cu.packageDecl != null) {
                import.prependPrefix("\n\n")
            } else {
                import.formatting = Formatting.Reified.Empty
                cu.typeDecls.firstOrNull()?.prependPrefix("\n\n")
            }
        }

        return super.visitImport(import)
    }

    private fun Tree.prependPrefix(prefix: String) {
        when(formatting) {
            is Formatting.Reified -> {
                val reified = formatting as Formatting.Reified
                reified.prefix = prefix + reified.prefix
            }
            is Formatting.Infer ->
                formatting = Formatting.Reified(prefix)
        }
    }
}
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import java.util.ArrayList
import com.netflix.java.refactor.refactor.RefactorVisitor

class AddImport(val clazz: String, val staticMethod: String? = null): RefactorVisitor() {
    private val imports = ArrayList<Tr.Import>()
    private var coveredByExistingImport = false
    private val packageComparator = PackageComparator()

    private lateinit var cu: Tr.CompilationUnit
    private val typeCache by lazy { TypeCache.of(cu.cacheId) }
    private val classType by lazy { Type.Class.build(typeCache, clazz) }

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): List<AstTransform<*>> {
        this.cu = cu
        return super.visitCompilationUnit(cu)
    }

    override fun visitImport(import: Tr.Import): List<AstTransform<*>> {
        imports.add(import)
        val importedType = import.qualid.name.name

        if (addingStaticImport()) {
            if (import.matches(clazz) && import.static && (importedType == staticMethod || importedType == "*")) {
                coveredByExistingImport = true
            }
        }
        else {
            if (import.matches(clazz)) {
                coveredByExistingImport = true
            } else if (import.qualid.target.printTrimmed() == classType.packageOwner() && importedType == "*") {
                coveredByExistingImport = true
            }
        }

        return emptyList()
    }

    override fun visitEnd(): List<AstTransform<*>> {
        if(classType.packageOwner().isEmpty())
            return emptyList()

        val lastPrior = lastPriorImport()
        val classImportField = TreeBuilder.buildName(typeCache, clazz, Formatting.Reified(" ")) as Tr.FieldAccess
        val cuCursor = Cursor(listOf(cu))

        val importStatementToAdd = if(addingStaticImport()) {
            Tr.Import(Tr.FieldAccess(classImportField, Tr.Ident(staticMethod!!, null, Formatting.Reified.Empty), null, Formatting.Reified.Empty), true, Formatting.Infer)
        } else Tr.Import(classImportField, false, Formatting.Infer)

        return if(coveredByExistingImport) {
            emptyList()
        }
        else if(lastPrior == null) {
            listOf(AstTransform(cuCursor, { cu: Tr.CompilationUnit -> cu.copy(imports = listOf(importStatementToAdd) + imports) }))
        }
        else {
            listOf(AstTransform(cuCursor, { cu: Tr.CompilationUnit -> cu.copy(imports =
                imports.takeWhile { it !== lastPrior } + listOf(lastPrior, importStatementToAdd) + imports.takeLastWhile { it !== lastPrior }) }))
        }
    }

    fun lastPriorImport(): Tr.Import? {
        return imports.lastOrNull { import ->
            // static imports go after all non-static imports
            if(addingStaticImport() && !import.static)
                return@lastOrNull true

            // non-static imports should always go before static imports
            if(!addingStaticImport() && import.static)
                return@lastOrNull false

            val comp = packageComparator.compare(import.qualid.target.printTrimmed(),
                    if(addingStaticImport()) clazz else classType.packageOwner())
            if(comp == 0) {
                if(import.qualid.name.toString().compareTo(if(addingStaticImport()) staticMethod!! else classType.className()) < 0) {
                    true
                }
                else false
            }
            else if(comp < 0) true
            else false
        }
    }

    fun addingStaticImport() = staticMethod is String
}
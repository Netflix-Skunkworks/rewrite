package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.asClass
import com.netflix.java.refactor.ast.visitor.FormatVisitor
import com.netflix.java.refactor.ast.visitor.TransformVisitor
import com.netflix.java.refactor.refactor.op.*
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import java.io.ByteArrayOutputStream
import java.util.*

class Refactor(val cu: Tr.CompilationUnit) {
    private val ops = ArrayList<RefactorVisitor>()

    fun addImport(clazz: Class<*>, staticMethod: String? = null) = addImport(clazz.name, staticMethod)

    fun addImport(clazz: String, staticMethod: String? = null): Refactor {
        ops.add(AddImport(cu, clazz, staticMethod))
        return this
    }

    fun removeImport(clazz: Class<*>) = removeImport(clazz.name)

    fun removeImport(clazz: String): Refactor {
        ops.add(RemoveImport(cu, clazz))
        return this
    }

    fun addField(target: Tr.ClassDecl, clazz: Class<*>, name: String, init: String?) = addField(target, clazz.name, name, init)

    fun addField(target: Tr.ClassDecl, clazz: Class<*>, name: String) = addField(target, clazz.name, name, null)

    fun addField(target: Tr.ClassDecl, clazz: String, name: String) = addField(target, clazz, name, null)

    fun addField(target: Tr.ClassDecl, clazz: String, name: String, init: String?): Refactor {
        ops.add(AddField(target, clazz, name, init))
        return this
    }

    fun changeType(target: Tr.VariableDecls, toType: Class<*>): Refactor = changeType(target, toType.name)

    fun changeType(target: Tr.VariableDecls, toType: String): Refactor {
        ops.add(ChangeFieldType(cu, target, toType))
        ops.add(AddImport(cu, toType))
        target.typeExpr.type?.asClass()?.let { ops.add(RemoveImport(cu, it.fullyQualifiedName)) }
        return this
    }

    fun delete(target: Tr.VariableDecls): Refactor {
        ops.add(DeleteField(cu, target))
        target.typeExpr.type?.asClass()?.let { ops.add(RemoveImport(cu, it.fullyQualifiedName)) }
        return this
    }

    /**
    * @return Transformed version of the AST after changes are applied
    */
    fun fix(): Tr.CompilationUnit {
        val fixed = ops.fold(cu) { acc, op ->
            // by transforming the AST for each op, we allow for the possibility of overlapping changes
            TransformVisitor(op.visit(acc)).visit(acc) as Tr.CompilationUnit
        }
        FormatVisitor().visit(fixed)
        return fixed
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit
     */
    fun diff() = InMemoryDiffEntry(cu.source.path, cu.print(), fix().print()).diff

    internal class InMemoryDiffEntry(filePath: String, old: String, new: String): DiffEntry() {
        private val repo = InMemoryRepository.Builder().build()

        init {
            changeType = ChangeType.MODIFY
            oldPath = filePath
            newPath = filePath

            val inserter = repo.objectDatabase.newInserter()
            oldId = inserter.insert(Constants.OBJ_BLOB, old.toByteArray()).abbreviate(40)
            newId = inserter.insert(Constants.OBJ_BLOB, new.toByteArray()).abbreviate(40)
            inserter.flush()

            oldMode = FileMode.REGULAR_FILE
            newMode = FileMode.REGULAR_FILE
            repo.close()
        }

        val diff: String by lazy {
            if(oldId == newId)
                ""
            else {
                val patch = ByteArrayOutputStream()
                val formatter = DiffFormatter(patch)
                formatter.setRepository(repo)
                formatter.format(this)
                String(patch.toByteArray())
            }
        }
    }
}
package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.nio.JavacPathFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import com.sun.tools.javac.util.Options
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class OracleJdkParser(classpath: List<Path>? = null) : Parser(classpath) {
    val context = Context()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with context
    private val compilerLog = object : Log(context) {
        fun removeFile(file: JavaFileObject) {
            sourceMap.remove(file)
        }
    }
    private val pfm = JavacPathFileManager(context, true, Charset.defaultCharset())

    private val compiler = JavaCompiler(context)

    companion object {
        private val logger = LoggerFactory.getLogger(OracleJdkParser::class.java)
    }

    init {
        // otherwise, consecutive string literals in binary expressions are concatenated by the parser, losing the original
        // structure of the expression!
        Options.instance(context).put("allowStringFolding", "false")

        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true
        compilerLog.setWriters(PrintWriter(object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                val log = String(cbuf.slice(off..(off + len - 1)).toCharArray())
                if(log.isNotBlank())
                    logger.warn(log)
            }

            override fun flush() {
            }

            override fun close() {
            }
        }))
    }

    override fun parse(sourceFiles: List<Path>): List<Tr.CompilationUnit> {
        if (filteredClasspath != null) { // override classpath
            assert(context.get(JavaFileManager::class.java) === pfm)
            pfm.setLocation(StandardLocation.CLASS_PATH, filteredClasspath)
        }

        val fileObjects = pfm.getJavaFileObjects(*filterSourceFiles(sourceFiles).toTypedArray())

        // otherwise, when the parser attempts to set endPosTable on the DiagnosticSource of the files it will blow up
        // because the previous parsing iteration has already set one
        fileObjects.forEach(compilerLog::removeFile)

        val cus = fileObjects.map { Paths.get(it.toUri()) to compiler.parse(it) }.toMap()

        try {
            cus.values.enterAll()
            compiler.attribute(compiler.todo)
        } catch(ignore: Throwable) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
        }

        return cus.map {
            val (path, cu) = it
            toIntermediateAst(cu, path, path.toFile().readText())
        }
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private fun Collection<JCTree.JCCompilationUnit>.enterAll() {
        val enter = Enter.instance(context)
        val compilationUnits = com.sun.tools.javac.util.List.from(this.toTypedArray())
        enter.main(compilationUnits)
    }

    private fun toIntermediateAst(cu: JCTree.JCCompilationUnit, path: Path, source: String): Tr.CompilationUnit {
        logger.trace("Building AST for {}", path.toAbsolutePath().fileName)
        return OracleJdkParserVisitor(path, source).scan(cu, Formatting.Reified.Empty) as Tr.CompilationUnit
    }
}

/*
class AstParser(val classpath: Iterable<Path>?) {
    val context = Context()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with contest
    private val mutableLog = MutableSourceMapLog()
    private val pfm = JavacPathFileManager(context, true, Charset.defaultCharset())

    val compiler = JavaCompiler(context)

    private inner class MutableSourceMapLog(): Log(context) {
        fun removeFile(file: JavaFileObject) {
            sourceMap.remove(file)
        }
    }

    private val logger = LoggerFactory.getLogger(AstParser::class.java)

    init {
        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true
        mutableLog.setWriters(PrintWriter(object: Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                logger.debug(String(cbuf.slice(off..(off + len)).toCharArray()))
            }
            override fun flush() {}
            override fun close() {}
        }))
    }

    fun parseFiles(files: Iterable<Path>): List<JCTree.JCCompilationUnit> {
        if(classpath != null) { // override classpath
            assert(context.get(JavaFileManager::class.java) === pfm)
            pfm.setLocation(StandardLocation.CLASS_PATH, classpath)
        }

        val fileObjects = pfm.getJavaFileObjects(*files.toList().toTypedArray())

        // if we are in a reparsing phase, we want to ensure that the contents of the file get re-read
        fileObjects.forEach { pfm.flushCache(it) }

        val cus = fileObjects.map { compiler.parse(it) }

        try {
            cus.enterAll()
            compiler.attribute(compiler.todo)
        } catch(ignore: Throwable) {
            // when symbol entering fails on problems like missing types, attribution can often times proceed
            // unhindered, but it sometimes cannot (so attribution is always a BEST EFFORT in the presence of errors)
        }

        return cus
    }

    fun reparse(cu: CompilationUnit): JCTree.JCCompilationUnit {
        // this will cause the new AST to be re-entered and re-attributed
        val chk = Check.instance(context)
        cu.jcCompilationUnit.defs.filterIsInstance<JCTree.JCClassDecl>().forEach {
            chk.compiled.remove(it.sym.flatname)
        }

        // otherwise, when the parser attempts to set endPosTable on the DiagnosticSource of this file it will blow up
        // because the previous parsing iteration has already set one
        mutableLog.removeFile(pfm.getJavaFileObjects(cu.source()).first())

        return parseFiles(listOf(cu.source())).first()
    }

    /**
     * Enter symbol definitions into each compilation unit's scope
     */
    private fun List<JCTree.JCCompilationUnit>.enterAll(): List<JCTree.JCCompilationUnit> {
        val enter = Enter.instance(context)
        val compilationUnits = com.sun.tools.javac.util.List.from(this.toTypedArray())
        enter.main(compilationUnits)
        return this
    }
}
 */

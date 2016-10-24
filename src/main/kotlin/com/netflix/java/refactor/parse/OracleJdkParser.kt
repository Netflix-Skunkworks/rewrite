package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.nio.JavacPathFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import javax.tools.JavaFileManager
import javax.tools.StandardLocation

class OracleJdkParser(classpath: List<Path>? = null) : Parser(classpath) {
    val context = Context()

    // Both of these must be declared before compiler, so that compiler doesn't attempt to register alternate
    // instances with contest
    private val compilerLog = object : Log(context) {}
    private val pfm = JavacPathFileManager(context, true, Charset.defaultCharset())

    private val compiler = JavaCompiler(context)

    private val logger = LoggerFactory.getLogger(OracleJdkParser::class.java)

    init {
        // otherwise the JavacParser will use EmptyEndPosTable, effectively setting -1 as the end position
        // for every tree element
        compiler.genEndPos = true
        compilerLog.setWriters(PrintWriter(object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                logger.info(String(cbuf.slice(off..(off + len)).toCharArray()))
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

    private fun toIntermediateAst(cu: JCTree.JCCompilationUnit, path: Path, source: String): Tr.CompilationUnit =
        OracleJdkParserVisitor(path, source).scan(cu, Formatting.Reified.Empty) as Tr.CompilationUnit
}

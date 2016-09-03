package com.netflix.java.refactor.tree

import com.netflix.java.refactor.ast.AstParser
import com.netflix.java.refactor.tree.oraclejdk.toAst
import java.io.File
import java.nio.file.Files

private val oracleJdk = { source: String, dependencies: List<String> ->
    val sourceDir = Files.createTempDirectory("sources").toFile()
    
    fun sourceFile(source: String): File {
        val sourceFile = File(sourceDir, "${simpleName(source)}.java")
        sourceFile.writeText(source.trimMargin())
        return sourceFile
    }

    val parser = AstParser(classpath = emptyList())
    val allFiles = dependencies.map(::sourceFile).plus(sourceFile(source))
    val cu = parser.parseFiles(allFiles.map { it.toPath() }).last().toAst()
    
    sourceDir.deleteRecursively()
    cu
}

class OracleJdkJRCompilationUnitTest: JRCompilationUnitTest(oracleJdk)
class OracleJdkJRImportTest: JRImportTest(oracleJdk)
class OracleJdkJRNewClassTest: JRNewClassTest(oracleJdk)
class OracleJdkJRClassDeclTest: JRClassDeclTest(oracleJdk)
class OracleJdkJRLiteralTest: JRLiteralTest(oracleJdk)
class OracleJdkJRIdentTest: JRIdentTest(oracleJdk)
class OracleJdkJRFieldAccessTest: JRFieldAccessTest(oracleJdk)
class OracleJdkJRMethodDeclTest: JRMethodDeclTest(oracleJdk)
class OracleJdkJRPrimitiveTest: JRPrimitiveTest(oracleJdk)
class OracleJdkJRBlockTest: JRBlockTest(oracleJdk)
class OracleJdkJRMethodInvocationTest: JRMethodInvocationTest(oracleJdk)
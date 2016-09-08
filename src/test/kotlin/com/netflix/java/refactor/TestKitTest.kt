package com.netflix.java.refactor

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.regex.Pattern

/**
 * Things that really belong in the unfinished Gradle Testkit
 */
abstract class TestKitTest {
    lateinit var projectDir: File
    lateinit var buildFile: File
    lateinit var settingsFile: File

    @JvmField @Rule
    val temp = TemporaryFolder()
    
    @Before
    fun setup() {
        projectDir = temp.root
        buildFile = File(projectDir, "build.gradle")
        settingsFile = File(projectDir, "settings.gradle")
    }
    
    fun runTasksSuccessfully(vararg tasks: String) =
        GradleRunner.create()
            .withDebug(true)
            .withProjectDir(projectDir)
            .withArguments(*tasks.toList().plus("--stacktrace").toTypedArray())
            .withPluginClasspath()
            .build()
    
    fun addSubproject(name: String): File {
        val subprojectDir = File(projectDir, name)
        subprojectDir.mkdirs()
        settingsFile.writeText("include '$name'\n")
        return subprojectDir
    }

    fun addSubproject(name: String, buildGradleContents: String): File {
        val subprojectDir = File(projectDir, name)
        subprojectDir.mkdirs()
        File(subprojectDir, "build.gradle").writeText(buildGradleContents)
        settingsFile.writeText("include '$name'\n")
        return subprojectDir
    }
  
    fun createJavaFile(projectDir: File, source: String, sourceFolderPath: String): File {
        val sourceFolder = File(projectDir, sourceFolderPath)
        sourceFolder.mkdirs()
        val f = File(sourceFolder, fullyQualifiedName(source)!!.replace("\\.", "/") + ".java")
        f.writeText(source)
        return f
    }
    
    fun createJavaTestFile(projectDir: File, source: String) =
            createJavaFile(projectDir, source, "src/test/java")
    
    fun createJavaTestFile(source: String) = createJavaTestFile(projectDir, source)
    
    fun createJavaSourceFile(projectDir: File, source: String) =
            createJavaFile(projectDir, source, "src/main/java")
    
    fun createJavaSourceFile(source: String) = createJavaSourceFile(projectDir, source)

    fun fullyQualifiedName(sourceStr: String): String? {
        val pkgMatcher = Pattern.compile("\\s*package\\s+([\\w\\.]+)").matcher(sourceStr)
        val pkg = if (pkgMatcher.find()) pkgMatcher.group(1) + "." else ""

        val classMatcher = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)").matcher(sourceStr)
        return if (classMatcher.find()) pkg + classMatcher.group(3) else null
    }
}

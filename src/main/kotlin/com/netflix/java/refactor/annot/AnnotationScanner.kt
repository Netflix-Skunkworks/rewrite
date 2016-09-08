package com.netflix.java.refactor.annot

import com.netflix.java.refactor.ast.AstVisitor
import com.netflix.java.refactor.refactor.RefactorTreeVisitor
import eu.infomas.annotation.AnnotationDetector
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*

object AnnotationScanner {
    private val logger = LoggerFactory.getLogger(AnnotationScanner::class.java)
    
    /**
     * Find all classes annotated with @AutoRefactor on the classpath that implement JavaSourceScanner.
     * Does not work on virtual file systems at this time.
     */
    fun allAutoRefactorsOnClasspath(classpath: Iterable<Path>): Map<AutoRefactor, RefactorTreeVisitor> {
        val scanners = HashMap<AutoRefactor, RefactorTreeVisitor>()
        val classLoader = URLClassLoader(classpath.map { it.toFile().toURI().toURL() }.toTypedArray(), javaClass.classLoader)

        val reporter = object: AnnotationDetector.TypeReporter {
            override fun annotations() = arrayOf(AutoRefactor::class.java)

            override fun reportTypeAnnotation(annotation: Class<out Annotation>, className: String) {
                val clazz = Class.forName(className, false, classLoader)
                val refactor = clazz.getAnnotation(AutoRefactor::class.java)

                try {
                    val scanner = clazz.newInstance()
                    if(scanner is RefactorTreeVisitor) {
                        scanners.put(refactor, scanner)
                    }
                    else {
                        logger.warn("To be useable, an @AutoRefactor must implement JavaSourceScanner or extend JavaSourceVisitor")
                    }
                } catch(ignored: ReflectiveOperationException) {
                    logger.warn("Unable to construct @AutoRefactor with id '${refactor.value}'. It must extend implement JavaSourceScanner or extend JavaSourceVisitor and have a zero argument public constructor.")
                }
            }
        }

        AnnotationDetector(reporter).detect(*classpath.map { it.toFile() }.toTypedArray())
        return scanners
    }
}
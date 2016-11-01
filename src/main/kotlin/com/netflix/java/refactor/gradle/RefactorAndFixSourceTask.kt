package com.netflix.java.refactor.gradle

import org.gradle.api.DefaultTask
import org.gradle.logging.StyledTextOutputFactory
import javax.inject.Inject

open class RefactorAndFixSourceTask : DefaultTask() {
    // see http://gradle.1045684.n5.nabble.com/injecting-dependencies-into-task-instances-td5712637.html
    @Inject
    open fun getTextOutputFactory(): StyledTextOutputFactory? = null
    
    private class RuleDescriptor(val name: String, val description: String)
    
    typealias RelativePath = String
    
//    @TaskAction
//    fun refactorSource() {
//        val fixesByRule = hashMapOf<RuleDescriptor, MutableSet<RelativePath>>()
//
//        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.forEach {
//            val sources = it.allJava.map { it.toPath() }
//            val classpath = it.compileClasspath.map { it.toPath() }
//
//            AnnotationScanner.allAutoRefactorsOnClasspath(classpath).forEach { refactor, visitor ->
//                OracleJdkParser(classpath).parse(sources).forEach { cu ->
//                    if(visitor.visit(cu).isNotEmpty()) {
//                        fixesByRule.getOrPut(RuleDescriptor(refactor.value, refactor.description), { HashSet<RelativePath>() }).add(cu.source.path)
//                    }
//                }
//            }
//        }
//
//        printReport(fixesByRule)
//    }
//
//    private fun printReport(fixesByRule: Map<RuleDescriptor, Collection<RelativePath>>) {
//        val textOutput = getTextOutputFactory()!!.create(RefactorAndFixSourceTask::class.java)
//
//        if(fixesByRule.isEmpty()) {
//            textOutput.style(Styling.Green).println("Passed refactoring check with no changes necessary")
//        } else {
//            textOutput.text("Refactoring operations were performed on this project. ")
//                    .withStyle(Styling.Bold).println("Please review the changes and commit.\n")
//
//            fixesByRule.entries.forEachIndexed { i, entry ->
//                val (rule, ruleFixes) = entry
//                textOutput.withStyle(Styling.Bold).text("${"${i+1}.".padEnd(2)} ${rule.description}")
//                textOutput.text(" (${ruleFixes.size} files changed) - ")
//                textOutput.withStyle(Styling.Yellow).println(rule.description)
//            }
//        }
//    }
}
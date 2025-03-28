package com.example

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.project.Project as IdeaProject
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Gradle plugin for analyzing Kotlin project structures
 */
class KotlinProjectAnalyzerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create a task for Kotlin project analysis
        project.tasks.register("analyzeKotlinProject") { task ->
            task.doLast {
                val kotlinSourceAnalyzer = KotlinSourceAnalyzer(project)
                kotlinSourceAnalyzer.analyze()
            }
        }
    }
}

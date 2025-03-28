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
 * Handles the core logic of analyzing Kotlin source structures
 */
class KotlinSourceAnalyzer(private val project: Project) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun analyze() {
        // Verify Kotlin plugin is applied
        if (!isKotlinPluginApplied()) {
            project.logger.error("Kotlin Gradle Plugin is not applied to this project.")
            return
        }

        // Collect Kotlin source files
        val kotlinSourceFiles = collectKotlinSourceFiles()

        // Analyze source files
        val projectStructure = analyzeSourceFiles(kotlinSourceFiles)

        // Output results
        outputAnalysisResults(projectStructure)
    }

    private fun isKotlinPluginApplied(): Boolean {
        return project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
    }

    private fun collectKotlinSourceFiles(): List<File> {
        return project.sourceSets.flatMap { sourceSet ->
            sourceSet.kotlin.sourceDirectories.files.filter { it.extension == "kt" }
        }
    }

    private fun analyzeSourceFiles(files: List<File>): ProjectStructure {
        val sourceStructures = files.map { file ->
            analyzeKotlinFile(file)
        }

        return ProjectStructure(
            totalFiles = files.size,
            sourceFiles = sourceStructures
        )
    }

    private fun analyzeKotlinFile(file: File): SourceFileStructure {
        val psiFile = PsiManager.getInstance(project as IdeaProject)
            .findFile(StandardFileSystems.local().findFileByPath(file.absolutePath)!!) as? KtFile
            ?: throw IllegalStateException("Could not parse Kotlin file: ${file.name}")

        return SourceFileStructure(
            fileName = file.name,
            packageName = psiFile.packageFqName.asString(),
            classes = psiFile.classes.map { ktClass ->
                ClassStructure(
                    name = ktClass.name ?: "Unknown",
                    type = when (ktClass) {
                        is KtClass -> ktClass.kotlinKeywordModifierType().toString()
                        else -> "Unknown"
                    },
                    functions = ktClass.declarations.filterIsInstance<KtNamedFunction>().map { func ->
                        FunctionStructure(
                            name = func.name ?: "Unknown",
                            visibility = func.visibility.toString(),
                            parameters = func.valueParameters.map { it.name ?: "Unknown" }
                        )
                    },
                    properties = ktClass.declarations.filterIsInstance<KtProperty>().map { prop ->
                        PropertyStructure(
                            name = prop.name ?: "Unknown",
                            type = prop.typeReference?.text ?: "Unknown",
                            visibility = prop.visibility.toString()
                        )
                    }
                )
            }
        )
    }

    private fun outputAnalysisResults(projectStructure: ProjectStructure) {
        val outputDir = File(project.buildDir, "kotlin-project-analysis")
        outputDir.mkdirs()

        // Output JSON file
        val jsonOutput = File(outputDir, "project-structure.json")
        jsonOutput.writeText(gson.toJson(projectStructure))

        // Output text summary
        val summaryOutput = File(outputDir, "project-structure-summary.txt")
        summaryOutput.writeText(generateTextSummary(projectStructure))

        project.logger.lifecycle("Kotlin project analysis complete. Results written to ${outputDir.absolutePath}")
    }

    private fun generateTextSummary(projectStructure: ProjectStructure): String {
        return buildString {
            appendLine("Kotlin Project Analysis Report")
            appendLine("==============================")
            appendLine("Total Source Files: ${projectStructure.totalFiles}")
            projectStructure.sourceFiles.forEach { sourceFile ->
                appendLine("\nFile: ${sourceFile.fileName}")
                appendLine("Package: ${sourceFile.packageName}")
                sourceFile.classes.forEach { cls ->
                    appendLine("  Class: ${cls.name} (${cls.type})")
                    appendLine("    Functions: ${cls.functions.size}")
                    appendLine("    Properties: ${cls.properties.size}")
                }
            }
        }
    }
}

// Data structures to represent project analysis results
data class ProjectStructure(
    val totalFiles: Int,
    val sourceFiles: List<SourceFileStructure>
)

data class SourceFileStructure(
    val fileName: String,
    val packageName: String,
    val classes: List<ClassStructure>
)

data class ClassStructure(
    val name: String,
    val type: String,
    val functions: List<FunctionStructure>,
    val properties: List<PropertyStructure>
)

data class FunctionStructure(
    val name: String,
    val visibility: String,
    val parameters: List<String>
)

data class PropertyStructure(
    val name: String,
    val type: String,
    val visibility: String
)
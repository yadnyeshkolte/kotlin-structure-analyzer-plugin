package com.example

import com.google.gson.GsonBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.io.File
import java.util.*

class KotlinStructureAnalyzerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // First check if Kotlin plugin is applied
        if (!project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            project.logger.warn("Kotlin plugin not found. KotlinStructureAnalyzerPlugin requires the Kotlin plugin to be applied.")
            return
        }

        // Define an extension for the plugin configuration
        val extension = project.extensions.create<KotlinStructureAnalyzerExtension>("kotlinStructureAnalyzer")

        // Add task to analyze Kotlin structure
        val analyzeTask = project.tasks.register("analyzeKotlinStructure") {
            group = "kotlin structure analyzer"
            description = "Analyzes the structure of Kotlin source files in the project"

            doLast {
                val sourceFiles = collectKotlinSourceFiles(project)
                if (sourceFiles.isEmpty()) {
                    logger.warn("No Kotlin source files found in the project.")
                    return@doLast
                }

                val structures = analyzeKotlinFiles(sourceFiles)
                val outputFile = extension.outputFile.get().asFile
                outputStructure(structures, outputFile, extension.outputFormat.get())
                logger.lifecycle("Kotlin structure analysis complete. Output written to ${outputFile.absolutePath}")
            }
        }

        // Make the task depend on compilation to ensure source files are up to date
        project.afterEvaluate {
            project.tasks.findByName("compileKotlin")?.let { compileTask ->
                analyzeTask.configure {
                    dependsOn(compileTask)
                }
            }
        }
    }

    private fun collectKotlinSourceFiles(project: Project): List<File> {
        val sourceFiles = mutableListOf<File>()

        // Get the source sets from Java plugin extension
        val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)
        if (javaExtension != null) {
            val mainSourceSet = javaExtension.sourceSets.findByName("main")
            mainSourceSet?.let { sourceSet ->
                // Find all Kotlin source directories
                val kotlinSrcDirs = sourceSet.allSource.srcDirs.filter { it.exists() }

                // Collect .kt files
                kotlinSrcDirs.forEach { dir ->
                    dir.walk().filter { it.isFile && it.extension == "kt" }.forEach {
                        sourceFiles.add(it)
                    }
                }
            }
        }

        return sourceFiles
    }

    private fun analyzeKotlinFiles(sourceFiles: List<File>): List<KotlinStructure> {
        val structures = mutableListOf<KotlinStructure>()

        // Set up the Kotlin environment
        val disposable = Disposer.newDisposable()
        try {
            val configuration = CompilerConfiguration()
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

            val environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val psiFileFactory = PsiFileFactory.getInstance(environment.project)

            // Process each source file
            for (sourceFile in sourceFiles) {
                val structure = KotlinStructure(
                    fileName = sourceFile.name,
                    path = sourceFile.absolutePath,
                    classes = mutableListOf(),
                    topLevelFunctions = mutableListOf(),
                    properties = mutableListOf()
                )

                val content = sourceFile.readText()
                val psiFile = psiFileFactory.createFileFromText(
                    sourceFile.name,
                    KotlinFileType.INSTANCE,
                    content
                ) as? KtFile

                if (psiFile != null) {
                    // Extract classes
                    psiFile.getChildrenOfType<KtClass>().forEach { ktClass ->
                        val classStructure = ClassStructure(
                            name = ktClass.name ?: "Anonymous",
                            isData = ktClass.isData(),
                            methods = ktClass.getChildrenOfType<KtNamedFunction>().map { function ->
                                FunctionStructure(
                                    name = function.name ?: "anonymous",
                                    parameters = function.valueParameters.map { param ->
                                        ParameterStructure(
                                            name = param.name ?: "",
                                            type = param.typeReference?.text ?: "Any"
                                        )
                                    }
                                )
                            },
                            properties = ktClass.getChildrenOfType<KtProperty>().map { property ->
                                PropertyStructure(
                                    name = property.name ?: "anonymous",
                                    type = property.typeReference?.text ?: "Any",
                                    isMutable = property.isVar
                                )
                            }
                        )
                        structure.classes.add(classStructure)
                    }

                    // Extract top-level functions
                    psiFile.getChildrenOfType<KtNamedFunction>().forEach { function ->
                        structure.topLevelFunctions.add(
                            FunctionStructure(
                                name = function.name ?: "anonymous",
                                parameters = function.valueParameters.map { param ->
                                    ParameterStructure(
                                        name = param.name ?: "",
                                        type = param.typeReference?.text ?: "Any"
                                    )
                                }
                            )
                        )
                    }

                    // Extract top-level properties
                    psiFile.getChildrenOfType<KtProperty>().forEach { property ->
                        structure.properties.add(
                            PropertyStructure(
                                name = property.name ?: "anonymous",
                                type = property.typeReference?.text ?: "Any",
                                isMutable = property.isVar
                            )
                        )
                    }
                }

                structures.add(structure)
            }
        } finally {
            Disposer.dispose(disposable)
        }

        return structures
    }

    private fun outputStructure(structures: List<KotlinStructure>, outputFile: File, format: String) {
        outputFile.parentFile?.mkdirs()

        when (format.lowercase(Locale.getDefault())) {
            "json" -> {
                val gson = GsonBuilder().setPrettyPrinting().create()
                outputFile.writeText(gson.toJson(structures))
            }
            "text" -> {
                val textBuilder = StringBuilder()
                structures.forEach { structure ->
                    textBuilder.appendLine("File: ${structure.fileName}")
                    textBuilder.appendLine("Path: ${structure.path}")
                    textBuilder.appendLine("Classes:")
                    structure.classes.forEach { classStructure ->
                        textBuilder.appendLine("  - ${classStructure.name}${if (classStructure.isData) " (data class)" else ""}")
                        textBuilder.appendLine("    Properties:")
                        classStructure.properties.forEach { property ->
                            textBuilder.appendLine("      - ${property.name}: ${property.type}${if (property.isMutable) " (var)" else " (val)"}")
                        }
                        textBuilder.appendLine("    Methods:")
                        classStructure.methods.forEach { method ->
                            textBuilder.appendLine("      - ${method.name}(${method.parameters.joinToString(", ") { "${it.name}: ${it.type}" }})")
                        }
                    }

                    if (structure.topLevelFunctions.isNotEmpty()) {
                        textBuilder.appendLine("Top-level Functions:")
                        structure.topLevelFunctions.forEach { function ->
                            textBuilder.appendLine("  - ${function.name}(${function.parameters.joinToString(", ") { "${it.name}: ${it.type}" }})")
                        }
                    }

                    if (structure.properties.isNotEmpty()) {
                        textBuilder.appendLine("Top-level Properties:")
                        structure.properties.forEach { property ->
                            textBuilder.appendLine("  - ${property.name}: ${property.type}${if (property.isMutable) " (var)" else " (val)"}")
                        }
                    }

                    textBuilder.appendLine()
                }
                outputFile.writeText(textBuilder.toString())
            }
            else -> {
                throw IllegalArgumentException("Unsupported output format: $format. Supported formats are 'json' and 'text'.")
            }
        }
    }
}
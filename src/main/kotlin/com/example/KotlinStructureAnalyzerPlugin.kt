package com.example

import com.google.gson.GsonBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.io.File
import java.util.*
import org.jetbrains.kotlin.config.CompilerConfiguration

class KotlinStructureAnalyzerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Define an extension for the plugin configuration
        val extension = project.extensions.create<KotlinStructureAnalyzerExtension>("kotlinStructureAnalyzer")

        // Add task to analyze Kotlin structure
        val analyzeTask = project.tasks.register("analyzeKotlinStructure") {
            group = "kotlin structure analyzer"
            description = "Analyzes the structure of Kotlin source files in the project"

            doLast {
                // Collect all Kotlin files in the project, regardless of source set structure
                val sourceFiles = collectKotlinSourceFiles(project)
                if (sourceFiles.isEmpty()) {
                    logger.warn("No Kotlin source files found in the project.")
                    return@doLast
                }

                logger.lifecycle("Found ${sourceFiles.size} Kotlin source files to analyze.")

                val structures = analyzeKotlinFiles(sourceFiles)
                val outputFile = extension.outputFile.get().asFile
                outputStructure(structures, outputFile, extension.outputFormat.get())
                logger.lifecycle("Kotlin structure analysis complete. Output written to ${outputFile.absolutePath}")
            }
        }

        // Make the task depend on all compile tasks to ensure source files are up to date
        project.afterEvaluate {
            // Find all compile tasks, including those in subprojects
            val compileTaskNames = project.tasks.names.filter {
                it.startsWith("compile") && (it.contains("Kotlin") || it.contains("kotlin"))
            }

            if (compileTaskNames.isNotEmpty()) {
                analyzeTask.configure {
                    dependsOn(compileTaskNames)
                }
                logger.debug("Added dependencies on compile tasks: $compileTaskNames")
            } else {
                logger.debug("No Kotlin compile tasks found")
            }
        }
    }

    private fun collectKotlinSourceFiles(project: Project): List<File> {
        val sourceFiles = mutableListOf<File>()
        val processedProjects = mutableSetOf<Project>()

        // Function to collect Kotlin files from a project and its subprojects
        fun collectFromProject(proj: Project) {
            if (proj in processedProjects) return
            processedProjects.add(proj)

            project.logger.debug("Collecting Kotlin source files from project: ${proj.name}")

            // Try multiple approaches to find Kotlin source files

            // 1. Try to get source sets from Java or Kotlin plugins
            try {
                val sourceSets = proj.extensions.findByName("sourceSets") as? SourceSetContainer
                sourceSets?.forEach { sourceSet ->
                    project.logger.debug("Processing source set: ${sourceSet.name}")

                    // Get Kotlin source directories
                    val kotlinDirs = sourceSet.allSource.srcDirs.filter { it.exists() }

                    // Collect all .kt files
                    kotlinDirs.forEach { dir ->
                        project.logger.debug("Scanning directory: ${dir.absolutePath}")
                        dir.walk()
                            .filter { it.isFile && it.extension == "kt" }
                            .forEach { sourceFiles.add(it) }
                    }
                }
            } catch (e: Exception) {
                project.logger.debug("Could not access sourceSets: ${e.message}")
            }

            // 2. If no source files found, try to find Kotlin files directly
            if (sourceFiles.isEmpty()) {
                project.logger.debug("No source files found via source sets, trying direct file search")

                // Collect all .kt files in src directory
                val srcDir = proj.file("src")
                if (srcDir.exists()) {
                    srcDir.walk()
                        .filter { it.isFile && it.extension == "kt" }
                        .forEach { sourceFiles.add(it) }
                }

                // Look for Kotlin files in specific common directories
                listOf(
                    "src/main/kotlin",
                    "src/commonMain/kotlin",
                    "src/androidMain/kotlin",
                    "src/iosMain/kotlin",
                    "src/desktopMain/kotlin",
                    "src/jsMain/kotlin",
                    "src/jvmMain/kotlin"
                ).forEach { path ->
                    val dir = proj.file(path)
                    if (dir.exists()) {
                        dir.walk()
                            .filter { it.isFile && it.extension == "kt" }
                            .forEach { sourceFiles.add(it) }
                    }
                }
            }

            // 3. Process subprojects as well
            proj.subprojects.forEach { subproject ->
                collectFromProject(subproject)
            }
        }

        // Start collection from root project
        collectFromProject(project)

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
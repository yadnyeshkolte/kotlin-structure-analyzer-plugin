package com.example

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinProjectAnalyzerPluginTest {
    @TempDir
    lateinit var testProjectDir: File

    private lateinit var buildFile: File
    private lateinit var settingsFile: File
    private lateinit var sourceFile: File

    @BeforeEach
    fun setup() {
        // Create build.gradle.kts
        buildFile = File(testProjectDir, "build.gradle.kts").apply {
            writeText("""
                plugins {
                    kotlin("jvm") version "1.9.22"
                    id("com.example.kotlinprojectanalyzer")
                }
                
                repositories {
                    mavenCentral()
                }
            """.trimIndent())
        }

        // Create settings.gradle.kts
        settingsFile = File(testProjectDir, "settings.gradle.kts").apply {
            writeText("""
                rootProject.name = "test-kotlin-project"
            """.trimIndent())
        }

        // Create a sample Kotlin source file
        val srcDir = File(testProjectDir, "src/main/kotlin").apply { mkdirs() }
        sourceFile = File(srcDir, "SampleClass.kt").apply {
            writeText("""
                package com.example.test
                
                class SampleClass {
                    val sampleProperty: String = "Hello"
                    
                    fun sampleFunction(param: Int): String {
                        return "Sample: ${'$'}param"
                    }
                }
            """.trimIndent())
        }
    }

    @Test
    fun `plugin can be applied and analyzeKotlinProject task runs successfully`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("analyzeKotlinProject")
            .withPluginClasspath()
            .build()

        // Check task outcome
        assertEquals(TaskOutcome.SUCCESS, result.task(":analyzeKotlinProject")?.outcome)

        // Verify output files are generated
        val buildDir = File(testProjectDir, "build/kotlin-project-analysis")
        assertTrue(buildDir.exists())

        val jsonOutput = File(buildDir, "project-structure.json")
        assertTrue(jsonOutput.exists())
        assertTrue(jsonOutput.length() > 0)

        val summaryOutput = File(buildDir, "project-structure-summary.txt")
        assertTrue(summaryOutput.exists())
        assertTrue(summaryOutput.length() > 0)
    }

    @Test
    fun `plugin fails if Kotlin plugin is not applied`() {
        // Create a build file without Kotlin plugin
        buildFile.writeText("""
            plugins {
                id("com.example.kotlinprojectanalyzer")
            }
            
            repositories {
                mavenCentral()
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("analyzeKotlinProject")
            .withPluginClasspath()
            .buildAndFail()

        // Verify the task fails due to missing Kotlin plugin
        assertTrue(result.output.contains("Kotlin Gradle Plugin is not applied to this project"))
    }
}
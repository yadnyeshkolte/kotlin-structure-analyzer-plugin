package com.example

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.example.KotlinStructureAnalyzerPlugin

class KotlinStructureAnalyzerPluginTest {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private fun getTestKitDir(): File {
        val gradleUserHome = System.getProperty("gradle.user.home")
        return if (gradleUserHome != null) {
            File(gradleUserHome)
        } else {
            testProjectDir.newFolder(".gradle")
        }
    }

    @Test
    fun `plugin applies correctly and generates report`() {
        // Set up test project
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("org.jetbrains.kotlin.jvm") version "1.9.20"
                id("com.example.kotlin-structure-analyzer")
            }
            
            repositories {
                mavenCentral()
            }
            
            kotlinStructureAnalyzer {
                outputFile.set(file("${"\$"}buildDir/kotlin-structure/report.json"))
                outputFormat.set("json")
            }
        """.trimIndent())

        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        settingsFile.writeText("""
            rootProject.name = "test-project"
            
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenLocal()
                }
            }
        """.trimIndent())

        // Create Kotlin source file for analysis
        val sourceDir = testProjectDir.newFolder("src", "main", "kotlin", "com", "example")
        val sourceFile = File(sourceDir, "TestClass.kt")
        sourceFile.writeText("""
            package com.example
            
            data class TestDataClass(
                val name: String,
                val age: Int,
                var address: String? = null
            ) {
                fun getInfo(): String = "${'$'}name, ${'$'}age years old"
            }
            
            class RegularClass {
                val property1: String = "test"
                var property2: Int = 42
                
                fun method1(param1: String, param2: Int = 0): Boolean {
                    return param1.length > param2
                }
            }
            
            fun topLevelFunction(input: String): String {
                return input.reversed()
            }
            
            val topLevelProperty = "Hello, World!"
        """.trimIndent())

        // Run the task
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("analyzeKotlinStructure", "--stacktrace")
            .withPluginClasspath()
            .withDebug(true)
            .forwardOutput()
            .build()

        // Check the outcome
        assertEquals(TaskOutcome.SUCCESS, result.task(":analyzeKotlinStructure")?.outcome)

        // Verify the report file was created
        val reportFile = File(testProjectDir.root, "build/kotlin-structure/report.json")
        assertTrue(reportFile.exists(), "Report file should be created")

        // Basic verification of report content
        val reportContent = reportFile.readText()
        assertTrue(reportContent.contains("TestDataClass"), "Report should include TestDataClass")
        assertTrue(reportContent.contains("RegularClass"), "Report should include RegularClass")
        assertTrue(reportContent.contains("topLevelFunction"), "Report should include top-level function")
        assertTrue(reportContent.contains("topLevelProperty"), "Report should include top-level property")
    }

    @Test
    fun `plugin generates text report`() {
        // Set up test project
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("org.jetbrains.kotlin.jvm") version "1.9.20"
                id("com.example.kotlin-structure-analyzer")
            }
            
            repositories {
                mavenCentral()
            }
            
            kotlinStructureAnalyzer {
                outputFile.set(file("${"\$"}buildDir/kotlin-structure/report.txt"))
                outputFormat.set("text")
            }
        """.trimIndent())

        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        settingsFile.writeText("""
            rootProject.name = "test-project"
            
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenLocal()
                }
            }
        """.trimIndent())

        // Create simple Kotlin source file
        val sourceDir = testProjectDir.newFolder("src", "main", "kotlin", "com", "example")
        val sourceFile = File(sourceDir, "Simple.kt")
        sourceFile.writeText("""
            package com.example
            
            class SimpleClass {
                fun doSomething() {}
            }
        """.trimIndent())

        // Run the task
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("analyzeKotlinStructure", "--stacktrace")
            .withPluginClasspath()
            .withDebug(true)
            .forwardOutput()
            .build()

        // Check the outcome
        assertEquals(TaskOutcome.SUCCESS, result.task(":analyzeKotlinStructure")?.outcome)

        // Verify the report file was created in text format
        val reportFile = File(testProjectDir.root, "build/kotlin-structure/report.txt")
        assertTrue(reportFile.exists(), "Text report file should be created")

        // Basic verification of report content
        val reportContent = reportFile.readText()
        assertTrue(reportContent.contains("SimpleClass"), "Report should include SimpleClass")
        //assertTrue(reportContent.contains("doSomething"), "Report should include the method name")
    }
}
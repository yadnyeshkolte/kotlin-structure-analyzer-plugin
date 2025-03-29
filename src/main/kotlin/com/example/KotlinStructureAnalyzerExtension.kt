package com.example

import org.gradle.api.Project
import org.gradle.kotlin.dsl.fileProperty
import org.gradle.kotlin.dsl.property

open class KotlinStructureAnalyzerExtension(project: Project) {
    val outputFile = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("reports/kotlin-structure/structure-report.json")
    )
    val outputFormat = project.objects.property(String::class.java).convention("json")
}
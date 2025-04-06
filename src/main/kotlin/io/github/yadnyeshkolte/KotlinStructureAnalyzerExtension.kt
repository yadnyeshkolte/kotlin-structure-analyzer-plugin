package io.github.yadnyeshkolte

import org.gradle.api.Project

open class KotlinStructureAnalyzerExtension(project: Project) {
    val outputFile = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("reports/kotlin-structure/structure-report.json")
    )
    val outputFormat = project.objects.property(String::class.java).convention("json")
}
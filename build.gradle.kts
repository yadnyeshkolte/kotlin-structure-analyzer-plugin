plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "io.github.yadnyeshkolte"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0") // Latest Kotlin
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.0") // Latest Kotlin Compiler
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0") // Latest Kotlin Gradle Plugin
    implementation("com.google.code.gson:gson:2.10.1") // Latest Gson

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.0") // Latest Kotlin Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0") // Latest Kotlin JUnit Test
    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/yadnyeshkolte/kotlin-structure-analyzer-plugin")
    vcsUrl.set("https://github.com/yadnyeshkolte/kotlin-structure-analyzer-plugin.git")
    plugins {
        create("kotlinStructureAnalyzerPlugin") {
            id = "io.github.yadnyeshkolte.kotlin-structure-analyzer"
            displayName = "Kotlin Structure Analyzer"
            description = "Analyzes the structure of Kotlin source files in Gradle projects"
            implementationClass = "io.github.yadnyeshkolte.KotlinStructureAnalyzerPlugin"
            tags.set(listOf("kotlin", "analysis", "structure"))
        }
    }
}

// Optional: Configure publishing if you want to publish to a repository
publishing {
    repositories {
        maven {
            name = "localRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

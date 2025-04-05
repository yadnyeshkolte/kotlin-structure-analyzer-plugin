plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "com.example"
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
    plugins {
        create("kotlinStructureAnalyzerPlugin") {
            id = "com.example.kotlin-structure-analyzer"
            implementationClass = "com.example.KotlinStructureAnalyzerPlugin"
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

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.20")
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "20"
    }
}
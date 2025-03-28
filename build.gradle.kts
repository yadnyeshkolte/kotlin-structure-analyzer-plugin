plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Kotlin Compiler Analysis Dependencies
    implementation("org.jetbrains.kotlin:kotlin-compiler:1.9.22")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

gradlePlugin {
    plugins {
        create("kotlinProjectAnalyzer") {
            id = "com.example.kotlinprojectanalyzer"
            implementationClass = "com.example.KotlinProjectAnalyzerPlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.example"
            artifactId = "kotlin-project-analyzer"
            version = "1.0.0"
        }
    }
}

kotlin {
    jvmToolchain(11)
}


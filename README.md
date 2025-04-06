# Kotlin Structure Analyzer Plugin Documentation

## Overview

The Kotlin Structure Analyzer is a Gradle plugin that analyzes the structure of Kotlin source files in your project. It provides detailed information about classes, functions, and properties in your Kotlin codebase, and generates a comprehensive report in either JSON or text format.

## Features

- Scans all Kotlin files in your project automatically
- Extracts detailed information about:
    - Classes (including data classes)
    - Methods and their parameters
    - Properties (with mutability information)
    - Top-level functions and properties
- Outputs reports in either JSON or text format
- Configurable output location and format
- Works with multi-module projects

## Installation

### Using the Gradle Plugin Portal

Add the plugin to your `build.gradle.kts` file:

```kotlin
plugins {
    id("io.github.yadnyeshkolte.kotlin-structure-analyzer") version "1.0.0"
}
```

Or in a traditional Gradle build file:

```groovy
plugins {
    id 'io.github.yadnyeshkolte.kotlin-structure-analyzer' version '1.0.0'
}
```

## Configuration

Configure the plugin using the `kotlinStructureAnalyzer` extension:

```kotlin
kotlinStructureAnalyzer {
    // Set output file location (defaults to build/reports/kotlin-structure/structure-report.json)
    outputFile.set(file("$buildDir/reports/kotlin-analysis/structure.json"))
    
    // Set output format: "json" or "text" (defaults to "json")
    outputFormat.set("json")
}
```

## Usage

Run the `analyzeKotlinStructure` task to generate the report:

```bash
./gradlew analyzeKotlinStructure
```

This will:
1. Scan all Kotlin source files in your project
2. Analyze their structure
3. Generate a report in the specified format
4. Save the report to the configured location

## Output Format

### JSON Output

The JSON output provides a structured representation of your Kotlin codebase:

```json
[
  {
    "fileName": "MyClass.kt",
    "path": "/path/to/MyClass.kt",
    "classes": [
      {
        "name": "MyClass",
        "isData": false,
        "methods": [
          {
            "name": "myMethod",
            "parameters": [
              {
                "name": "param1",
                "type": "String"
              }
            ]
          }
        ],
        "properties": [
          {
            "name": "myProperty",
            "type": "Int",
            "isMutable": false
          }
        ]
      }
    ],
    "topLevelFunctions": [...],
    "properties": [...]
  }
]
```

### Text Output

The text output provides a more human-readable format:

```
File: MyClass.kt
Path: /path/to/MyClass.kt
Classes:
  - MyClass
    Properties:
      - myProperty: Int (val)
    Methods:
      - myMethod(param1: String)
Top-level Functions:
  - topLevelFunction(input: String)
Top-level Properties:
  - topLevelProperty: String (val)
```

## Use Cases

The Kotlin Structure Analyzer is particularly useful for:

1. **Code Documentation**: Automatically generate API documentation based on your code structure
2. **Code Quality Analysis**: Identify patterns and anti-patterns in your codebase
3. **Code Reviews**: Get a quick overview of code structure before a review
4. **Project Metrics**: Gather statistics about your project's composition
5. **Refactoring Planning**: Identify areas that might benefit from refactoring
6. **Dependency Analysis**: Understand relationships between classes and functions

## Example: Visualizing Codebase Structure

You can use the JSON output to create visualizations of your codebase structure:

```kotlin
val structureJson = File("build/reports/kotlin-structure/structure-report.json").readText()
val structure = Gson().fromJson(structureJson, Array<KotlinStructure>::class.java)

// Analyze and visualize the structure
val totalClasses = structure.sumOf { it.classes.size }
val totalFunctions = structure.sumOf { 
    it.classes.sumOf { c -> c.methods.size } + it.topLevelFunctions.size 
}
val totalProperties = structure.sumOf { 
    it.classes.sumOf { c -> c.properties.size } + it.properties.size 
}
```

## Example: Generating Documentation

```kotlin
val structureJson = File("build/reports/kotlin-structure/structure-report.json").readText()
val structure = Gson().fromJson(structureJson, Array<KotlinStructure>::class.java)

// Generate markdown documentation
val mdBuilder = StringBuilder()
mdBuilder.appendLine("# Project API Documentation")

structure.forEach { file ->
    file.classes.forEach { cls ->
        mdBuilder.appendLine("## ${cls.name}${if (cls.isData) " (data class)" else ""}")
        mdBuilder.appendLine("\n### Properties\n")
        cls.properties.forEach { prop ->
            mdBuilder.appendLine("- `${prop.name}: ${prop.type}`${if (prop.isMutable) " (mutable)" else ""}")
        }
        mdBuilder.appendLine("\n### Methods\n")
        cls.methods.forEach { method ->
            mdBuilder.appendLine("- `${method.name}(${method.parameters.joinToString(", ") { "${it.name}: ${it.type}" }})`")
        }
    }
}
```

## Integration with CI/CD

Add the plugin to your CI/CD pipeline to track changes in your codebase structure:

```yaml
# Example GitHub Actions workflow
jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Analyze Kotlin Structure
        run: ./gradlew analyzeKotlinStructure
      - name: Upload analysis results
        uses: actions/upload-artifact@v3
        with:
          name: kotlin-structure-report
          path: build/reports/kotlin-structure/structure-report.json
```

## Troubleshooting

### No Kotlin Files Found

If the plugin reports that no Kotlin files were found:

1. Verify that your project contains Kotlin files
2. Check that the files are in standard source directories (src/main/kotlin, etc.)
3. Make sure you're running the task from the correct project directory

### Parsing Errors

If you encounter parsing errors:

1. Make sure your Kotlin files are syntactically correct
2. Check that you're using a compatible Kotlin version (the plugin supports Kotlin 1.7.0+)
3. Try running with `--stacktrace` to get more detailed error information

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
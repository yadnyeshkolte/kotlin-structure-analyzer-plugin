package com.example

data class KotlinStructure(
    val fileName: String,
    val path: String,
    val classes: MutableList<ClassStructure>,
    val topLevelFunctions: MutableList<FunctionStructure>,
    val properties: MutableList<PropertyStructure>
)
package com.github.johnnyjayjay.javadox

data class DocumentedType(
    val `package`: String,
    val uri: String,
    val type: String,
    val name: String,
    val inheritance: List<String>,
    val topTags: List<Pair<String, List<String>>>,
    val declaration: String,
    val description: String,
    val deprecation: String?,
    val bottomTags: List<Pair<String, List<String>>>,
    val inheritedMethods: Map<String, List<String>>,
    val enumConstants: List<DocumentedMember>,
    val fields: List<DocumentedMember>,
    val constructors: List<DocumentedMember>,
    val methods: List<DocumentedMember>
)

data class DocumentedMember(
    val uri: String,
    val name: String,
    val declaration: String,
    val description: String,
    val deprecation: String?,
    val tags: List<Pair<String, List<String>>>
)
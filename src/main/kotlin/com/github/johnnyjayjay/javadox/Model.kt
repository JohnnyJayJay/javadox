package com.github.johnnyjayjay.javadox

interface Documented {
  val name: String
  val uri: String
  val declaration: String
  val description: String
  val deprecation: String?
  val tags: List<Pair<String, List<String>>>
}

data class DocumentedType(
    val `package`: String,
    override val uri: String,
    val type: String,
    override val name: String,
    val inheritance: List<String>,
    val topTags: List<Pair<String, List<String>>>,
    override val declaration: String,
    override val description: String,
    override val deprecation: String?,
    override val tags: List<Pair<String, List<String>>>,
    val inheritedMethods: Map<String, List<String>>,
    val enumConstants: List<DocumentedMember>,
    val fields: List<DocumentedMember>,
    val constructors: List<DocumentedMember>,
    val methods: List<DocumentedMember>
) : Documented

data class DocumentedMember(
    override val uri: String,
    override val name: String,
    override val declaration: String,
    override val description: String,
    override val deprecation: String?,
    override val tags: List<Pair<String, List<String>>>
) : Documented
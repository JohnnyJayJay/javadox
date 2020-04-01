package com.github.johnnyjayjay.javadox

import org.jsoup.nodes.Document

class Javadocs(
    tree: String,
    index: String? = null,
    private val parser: JavadocParser,
    private val scrape: (String) -> Document
) {

  private val tree = scrape(tree).selectFirst("body .contentContainer")
  private val index = index?.also { scrape(it) }

  fun find(`package`: String = "", type: String): Sequence<DocumentedType> {
    val typeHref = buildString {
      if (`package`.isNotBlank()) {
        append(`package`.replace('.', '/').toLowerCase())
      }
      append('/')
      append(type.toLowerCase())
      append(".html")
    }
    val elements = tree.select("a[href]")
    return elements.asSequence()
        .filter { it.absUrl("href").toLowerCase().endsWith(typeHref) }
        .map { scrape(it.absUrl("href")) }
        .map { parser.parse(it) }
  }

  fun search(query: String): Sequence<String> {
    TODO()
  }

}

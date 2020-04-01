package com.github.johnnyjayjay.javadox

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Javadocs(
    tree: String,
    index: String? = null,
    private val parser: JavadocParser,
    private val scrape: (String) -> Document
) {

  private val tree = scrape(tree)
      .selectFirst("body .contentContainer")
      .apply(Element::replaceRelativeUris)
  private val index = index?.let(scrape)?.apply(Document::replaceRelativeUris)

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

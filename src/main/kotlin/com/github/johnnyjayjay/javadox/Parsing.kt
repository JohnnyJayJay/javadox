package com.github.johnnyjayjay.javadox

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class JavadocParser(val htmlConverter: (String) -> String = { it }) {

  fun parse(document: Document)
      = document.apply(Document::replaceRelativeUris).extractType()

  private fun Document.extractType(): DocumentedType {
    val packageTag = selectFirst("body > main > div.header span.packageLabelInType + a[href]")
    val `package` = htmlConverter(packageTag.outerHtml())
    val packageUri = packageTag.attr("href")
    val typeTitle = selectFirst("body > main > div.header > h2").text()
    val name = typeTitle.substringAfter(' ')
    val type = typeTitle.substringBefore(' ')
    val uri = "${packageUri.substringBeforeLast('/')}/${name.substringBefore('<')}.html"
    val inheritanceUl = selectFirst("body > main > div.contentContainer > ul.inheritance")
    val inheritance = inheritanceUl.parseInheritance()
    val descriptionBlock = selectFirst("body > main > div.contentContainer > div.description > ul > li")
    val description = htmlConverter(descriptionBlock.selectFirst("div.block")?.html() ?: "")
    val declaration = htmlConverter(descriptionBlock.selectFirst("pre").outerHtml())
    val topTags = descriptionBlock.children().asSequence()
        .takeWhile { it.tagName() == "dl" }
        .map { it.parseTags() }
        .reduce { acc, mutableList -> acc.apply { addAll(mutableList) } }
    val deprecation = descriptionBlock.parseDeprecation()
    val bottomTags = descriptionBlock.selectFirst("div.block + dl")?.parseTags() ?: mutableListOf()
    val inheritedMethodsList = select("body > main > div.contentContainer > div.summary > ul > li table.memberSummary ~ ul.blocklist")
    val inheritedMethods = inheritedMethodsList.parseInheritedMethods()
    val details = selectFirst("body > main > div.contentContainer > div.details > ul > li")
    val sections = if (details.selectFirst("section") != null) {
      details.select("section > ul > li")
    } else {
      details.select("> ul > li")
    }
    val enumConstants = sections.parseDetails(uri,"enum.constant.detail")
    val fields = sections.parseDetails(uri, "field.detail")
    val constructors = sections.parseDetails(uri, "constructor.detail")
    val methods = sections.parseDetails(uri, "method.detail")
    return DocumentedType(
        `package`, uri, type, name, inheritance, topTags, declaration, description, deprecation,
        bottomTags, inheritedMethods, enumConstants, fields, constructors, methods
    )
  }

  private fun Elements.parseDetails(typeUri: String, id: String): List<DocumentedMember> {
    val details = select("a[id=$id]").first()?.parent() ?: return emptyList()
    return details.select("a + ul")
        .map {
          val a = it.previousElementSibling()
          val name = (if (a.hasAttr("name")) a.attr("name") else a.attr("id"))
            .replaceFirst('-', '(').replace('-', ')')
          val uri = "$typeUri#${name.replace('(', '-').replace(')', '-')}"
          val declaration = htmlConverter(it.selectFirst("pre").outerHtml())
          val description = htmlConverter(it.selectFirst("div.block")?.html() ?: "")
          val tags = it.selectFirst("dl")?.parseTags() ?: mutableListOf()
          val deprecation = it.parseDeprecation()
          DocumentedMember(uri, name, declaration, description, deprecation, tags)
        }
  }

  private fun Element.parseDeprecation(): String? {
    if (selectFirst(".deprecatedLabel") != null) {
      return selectFirst(".deprecationComment")?.html()?.also { htmlConverter(it) } ?: ""
    }
    return null
  }

  private fun Elements.parseInheritedMethods(): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    for (element in this) {
      val title = element.selectFirst("li.blockList > h3")
      val key = htmlConverter(title.html().substringAfter("Methods declared in "))
      val methods = element.selectFirst("li.blockList > code")
      val value = methods.html().split(", ").map { "<code>$it</code>" }.map(htmlConverter)
      map[key] = value
    }
    return map
  }

  private fun Element.parseInheritance(): List<String> {
    val list = mutableListOf<String>()
    var current = selectFirst("li")
    while (current != null) {
      list.add(htmlConverter(current.html()))
      current = current.selectFirst("li > ul.inheritance > li")
    }
    return list
  }

  private fun Element.parseTags(): MutableList<Pair<String, List<String>>> {
    val result = mutableListOf<Pair<String, List<String>>>()
    val iterator = children().iterator()
    if (!iterator.hasNext()) {
      return mutableListOf()
    }
    var currentTag = iterator.next()
    var currentTagValues = mutableListOf<String>()
    while (iterator.hasNext()) {
      val child = iterator.next()
      when (child.tagName()) {
        "dt" -> {
          result.add(currentTag.text() to currentTagValues)
          currentTag = child
          currentTagValues = mutableListOf()
        }
        "dd" -> currentTagValues.add(htmlConverter(child.html()))
      }
    }
    result.add(currentTag.text() to currentTagValues)
    return result
  }
}

fun Element.replaceRelativeUris() {
  allElements.asSequence()
      .filter { it.tagName() == "a" && it.hasAttr("href") }
      .forEach { it.attr("href", it.absUrl("href")) }
}



package com.github.johnnyjayjay.javadox

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import kotlin.streams.asStream

// TODO different implementatiions based on javadoc version
class JavadocParser(val htmlConverter: (String) -> String = { it }) {

  fun parse(document: Document)
      = document.apply(Document::replaceRelativeUris).extractType()

  private fun Document.extractType(): DocumentedType {
    val packageTag = select("body div.header > div.subTitle").last()
    packageTag?.selectFirst("span.packageLabelInType")?.remove()
    val `package` = htmlConverter(packageTag?.html() ?: "").trim()
    val packageUri = packageTag?.selectFirst("a[href]")?.absUrl("href") ?: ""
    val typeTitle = selectFirst("body div.header > h2, body div.header > h1").text()
    val name = typeTitle.substringAfter(' ')
    val type = typeTitle.substringBefore(' ')
    val uri = "${packageUri.substringBeforeLast('/')}/${name.substringBefore('<')}.html"
    val inheritanceUl = selectFirst("body .contentContainer > .inheritance")
    val inheritance = inheritanceUl?.parseInheritance() ?: emptyList()
    val descriptionBlock = selectFirst("body div.contentContainer > .description")
    val description = htmlConverter(descriptionBlock.selectFirst("div.block")?.html() ?: "")
    val declaration = htmlConverter(descriptionBlock.selectFirst("pre").outerHtml())
    val topTags = descriptionBlock.select("dl").stream()
        .map { it.parseTags() }
        .reduce(mutableListOf()) { one, two -> one.apply { addAll(two) } }
    val deprecation = descriptionBlock.parseDeprecation()
    val bottomTags = descriptionBlock.selectFirst("div.block + dl")?.parseTags() ?: mutableListOf()
    val inheritedMethodsList = select("body div.contentContainer > .summary > ul > li table.memberSummary ~ ul.blocklist")
    val inheritedMethods = inheritedMethodsList.parseInheritedMethods()
    val details = selectFirst("body div.contentContainer > .details")
    val sections = if (details.selectFirst("ul > li > section") != null) {
      details.select("> ul > li > section")
    } else if (details.selectFirst("section") != null) {
      details.select("> ul > li section > ul > li")
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
    val details = select("a[id=$id], a[name=$id], a[name=${id.replace('.', '_')}]").first()
        ?.parent() ?: return emptyList()
    return details.select("a[id], a[name]")
        .map {
          val name = (if (it.hasAttr("name")) it.attr("name") else it.attr("id"))
            .replaceFirst('-', '(').replace('-', ')')
          if (name == id || name == id.replace('.', '_')) {
            return@map null
          }
          val detail = it.parent().let { if (it.tagName() == "h3") it.parent() else it }
          val uri = "$typeUri#${name.replace('(', '-').replace(')', '-')}"
          val declaration = htmlConverter((detail.selectFirst("pre") ?: detail.selectFirst(".memberSignature")).outerHtml())
          val description = htmlConverter(detail.selectFirst("div.block")?.html() ?: "")
          val tags = detail.selectFirst("dl")?.parseTags() ?: mutableListOf()
          val deprecation = detail.parseDeprecation()
          DocumentedMember(uri, name, declaration, description, deprecation, tags)
        }
        .filterNotNull()
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

  // FIXME 12
  private fun Element.parseInheritance(): List<String> {
    val list = mutableListOf<String>()
    html(html().trim())
    var current = childNode(0)
    do  {
      list.add(htmlConverter(current.outerHtml()).trim())
      if (current !is Element)
        break
      current.html(current.html().trim())
      current = current.parent().select(".inheritance").last().childNode(0)
    } while (current != null);
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



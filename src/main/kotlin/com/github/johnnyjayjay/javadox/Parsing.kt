package com.github.johnnyjayjay.javadox

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

// TODO different implementatiions based on javadoc version
class JavadocParser(val htmlConverter: (String) -> String = { it }) {

    fun parse(document: Document) = document.apply(Document::replaceRelativeUris).extractType()

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
        val inheritedMethodsList =
            select("body div.contentContainer > .summary > ul > li table.memberSummary ~ ul.blocklist")
        val inheritedMethods = inheritedMethodsList.parseInheritedMethods()
        val details = selectFirst("body div.contentContainer > .details")
        val sections = when {
            details.selectFirst("ul > li > section") != null -> details.select("> ul > li > section")
            details.selectFirst("section") != null -> details.select("> ul > li section > ul > li")
            else -> details.select("> ul > li")
        }
        val enumConstants = sections.parseDetails(uri, "enum.constant.detail")
        val fields = sections.parseDetails(uri, "field.detail")
        val constructors = sections.parseDetails(uri, "constructor.detail")
        val methods = sections.parseDetails(uri, "method.detail")
        return DocumentedType(
            `package`, uri, type, name, inheritance, topTags, declaration, description, deprecation,
            bottomTags, inheritedMethods, enumConstants, fields, constructors, methods
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun Elements.parseDetails(typeUri: String, id: String): List<DocumentedMember> {
        val details = select("a[id=$id], a[name=$id], a[name=${id.replace('.', '_')}]").first()
            ?.parent() ?: return emptyList()
        // block: codeblocks inside of descriptions like here https://cr.openjdk.java.net/~iris/se/10/latestSpec/api/java/util/List.html#toArray(T%5B%5D)
        // but since those always are wrapped in a block container we cann simply ignore those
        val declarations = details.select("pre").filter { "blockList" in it.parent().classNames() }
        return details.select("a[id], a[name], ul")
            // Stupid oracle decided it would be clever to always put an a for Object and generic type above methods with generic type so we only care about first a thing
            .scanReduce { previous, next ->
                if(next.tagName() != "a") return@scanReduce next
                val previousName = (previous?.attr("id") ?: previous?.attr("name"))?.substringBefore('(') //only get name
                val nextName = (next.attr("id") ?: next.attr("name")).substringBefore('(')
                if (!previousName.isNullOrBlank() && nextName.startsWith(previousName)) return@scanReduce null
                next
            }
            .mapIndexedNotNull { index, it ->
                if (it == null || it.tagName() == "ul") return@mapIndexedNotNull null
                val name = (if (it.hasAttr("name")) it.attr("name") else it.attr("id"))
                    .replaceFirst('-', '(').replace('-', ')')
                if (name == id || name == id.replace('.', '_')) null
                else it to name
            }
            .mapIndexed { index, (it, name) ->
                val detail = it.parent().let { if (it.tagName() == "h3") it.parent() else it }
                val uri = "$typeUri#${name.replace('(', '-').replace(')', '-')}"
                val declaration =
                    (declarations.getOrNull(index) ?: detail.selectFirst(".memberSignature"))?.outerHtml()
                        ?.let(htmlConverter) ?: ""
                val description = detail.select("div.block").getOrNull(index)?.html()?.let(htmlConverter) ?: ""
                val tags = detail.select("dl").getOrNull(index)?.parseTags() ?: mutableListOf()
                val deprecation = detail.parseDeprecation()
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

    // FIXME 12
    private fun Element.parseInheritance(): List<String> {
        val list = mutableListOf<String>()
        html(html().trim())
        var current = childNode(0)
        do {
            list.add(htmlConverter(current.outerHtml()).trim())
            if (current !is Element)
                break
            current.html(current.html().trim())
            current = current.parent().select(".inheritance").last().childNode(0)
        } while (current != null)
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

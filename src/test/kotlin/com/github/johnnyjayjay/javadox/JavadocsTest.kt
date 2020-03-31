package com.github.johnnyjayjay.javadox

import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JavadocsTest {

  lateinit var javadocs: Javadocs

  @BeforeEach
  internal fun setUp() {
    javadocs = Javadocs(
        allClasses = "https://docs.oracle.com/en/java/javase/12/docs/api/overview-tree.html",
        parser = JavadocParser()
    ) { Jsoup.connect(it).userAgent("Mozilla").get() }
  }

  @Test
  internal fun testParsing() {
    val type = javadocs.find(type = "List").first()
    type.methods.map { it.name }.forEach(::println)
  }
}
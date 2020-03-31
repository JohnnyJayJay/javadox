package com.github.johnnyjayjay.javadox

import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JavadocsTest {

  lateinit var javadocs: Javadocs

  @BeforeEach
  internal fun setUp() {
    javadocs = Javadocs(
        allClasses = "https://docs.oracle.com/javase/10/docs/api/allclasses-noframe.html",
        parser = JavadocParser()
    ) { Jsoup.connect(it).userAgent("Mozilla").get() }
  }

  @Test
  internal fun testParsing() {
    val type = javadocs.find("java.util", "List").first()
    type.methods.map { it.name }.forEach(::println)
  }
}
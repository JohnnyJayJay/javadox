package com.github.johnnyjayjay.javadox

import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JavadocsTest {

  lateinit var javadocs: Javadocs

  @BeforeEach
  internal fun setUp() {
    javadocs = Javadocs(
        allClasses = "https://helpch.at/docs/1.8.8/allclasses-noframe.html",
        parser = JavadocParser()
    ) { Jsoup.connect(it).userAgent("Mozilla").get() }
  }

  @Test
  internal fun testParsing() {
    val type = javadocs.find( type = "Achievement").first()
    type.methods.map { it.name }.forEach(::println)
  }
}
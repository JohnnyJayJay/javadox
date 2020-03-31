# Javadox
A small Kotlin library to parse HTML javadocs.

```kotlin
val allClassesUrl = "https://docs.oracle.com/javase/10/docs/api/allclasses-noframe.html"
val parser = JavadocParser()
val javadocs = Javadocs(allClasses = allClassesUrl, parser = parser) { 
  Jsoup.connect(it).userAgent("Mozilla").get() 
}
val javadoc: DocumentedType = javadocs.find(`package` = "java.util", type = "List")
```

Still testing and under construction.

Setup:
```
git clone https://github.com/johnnyjayjay/javadox.git
cd javadox
gradlew clean publishToMavenLocal
```
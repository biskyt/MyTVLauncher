package com.biskyt.mytvlauncher

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every string the launcher shows has Spanish, Brazilian Portuguese and French words (see CLAUDE.md › Languages). */
class StringsTest {
    private fun names(folder: String): Set<String> {
        val dir = File("src/main/res/$folder")
        return dir.listFiles { f -> f.name.startsWith("strings") }.orEmpty().flatMap { file ->
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            (0 until nodes.length).map { nodes.item(it) }
                .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
                .map { it.attributes.getNamedItem("name").nodeValue }
        }.toSet()
    }

    @Test
    fun `every string is translated`() {
        val english = names("values")
        listOf("values-es", "values-pt-rBR", "values-fr").forEach { assertEquals(it, english, names(it)) }
    }

    @Test
    fun `US English changes only strings that UK English also lists`() {
        assertEquals(names("values-en-rUS"), names("values-en-rGB"))
    }
}

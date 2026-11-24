package de.mm20.launcher2.data

import org.junit.Assert
import org.junit.Test
import java.util.Locale

class CompatStringNormalizerTest {

    private fun normalizeWith(locale: Locale, input: String): String {
        val defaultLocale = Locale.getDefault()
        return try {
            Locale.setDefault(locale)
            CompatStringNormalizer().normalize(input)
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    private val locales = listOf(Locale.ENGLISH, Locale.forLanguageTag("tr-TR"), Locale.GERMAN)

    @Test
    fun turkishLettersFoldToAscii() {
        val expected = mapOf(
            "ismail" to listOf("ismail", "Ismail", "İsmail", "ısmail", "ISMAIL"),
            "isik" to listOf("isik", "Işık", "ışık", "IŞIK"),
            "sule" to listOf("Şule", "sule"),
            "gul" to listOf("Gül", "GÜL"),
            "cagri" to listOf("Çağrı"),
        )
        for (locale in locales) {
            for ((normalized, inputs) in expected) {
                for (input in inputs) {
                    Assert.assertEquals("$input ($locale)", normalized, normalizeWith(locale, input))
                }
            }
        }
    }

    @Test
    fun otherLatinLetters() {
        for (locale in locales) {
            Assert.assertEquals("strasse", normalizeWith(locale, "Straße"))
            Assert.assertEquals("muller", normalizeWith(locale, "MÜLLER"))
            Assert.assertEquals("francois", normalizeWith(locale, "François"))
            Assert.assertEquals("aesop", normalizeWith(locale, "Æsop"))
        }
    }

    @Test
    fun outputDoesNotDependOnLocale() {
        val inputs = listOf("ISTANBUL", "İzmir", "Iğdır", "Ångström", "Иван", "张三")
        for (input in inputs) {
            val results = locales.map { normalizeWith(it, input) }.toSet()
            Assert.assertEquals(input, 1, results.size)
        }
    }
}

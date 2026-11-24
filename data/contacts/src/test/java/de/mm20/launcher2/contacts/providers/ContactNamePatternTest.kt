package de.mm20.launcher2.contacts.providers

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

/**
 * Evaluates a pattern from [ContactNamePattern.glob] the way SQLite's `GLOB` would, by translating
 * it to a regex. Only supports what [ContactNamePattern] emits: `*` and `[...]` classes without
 * ranges or negation.
 */
private fun globMatches(glob: String, text: String): Boolean {
    val regex = StringBuilder()
    var i = 0
    while (i < glob.length) {
        when (val c = glob[i]) {
            '*' -> regex.append(".*")
            '?' -> regex.append('.')
            '[' -> {
                val end = glob.indexOf(']', i + 2)
                regex.append('(')
                glob.substring(i + 1, end).codePoints().toArray().joinTo(regex, "|") {
                    Regex.escape(String(Character.toChars(it)))
                }
                regex.append(')')
                i = end
            }
            else -> regex.append(Regex.escape(c.toString()))
        }
        i++
    }
    return Regex(regex.toString(), RegexOption.DOT_MATCHES_ALL).matches(text)
}

@RunWith(Parameterized::class)
class ContactNamePatternTest(
    private val query: String,
    private val name: String,
    private val expected: Boolean,
) {

    @Test
    fun matchesWithEnglishLocale() = assertMatches(Locale.ENGLISH)

    @Test
    fun matchesWithTurkishLocale() = assertMatches(Locale.forLanguageTag("tr-TR"))

    private fun assertMatches(locale: Locale) {
        val defaultLocale = Locale.getDefault()
        try {
            Locale.setDefault(locale)
            val glob = ContactNamePattern.glob(query)
            Assert.assertEquals(
                "\"$query\" in \"$name\" ($locale, pattern $glob)",
                expected,
                globMatches(glob, name),
            )
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} in {1} = {2}")
        fun data(): List<Array<Any>> = listOf(
            // Turkish dotted and dotless i, in every combination.
            arrayOf("ismail", "İsmail", true),
            arrayOf("Ismail", "İsmail", true),
            arrayOf("İsmail", "İsmail", true),
            arrayOf("ısmail", "İsmail", true),
            arrayOf("ISMAIL", "İsmail", true),
            arrayOf("ismail", "Ismail", true),
            arrayOf("ismail", "ISMAIL", true),
            arrayOf("isik", "Işık", true),
            arrayOf("ışık", "Işık", true),
            arrayOf("IŞIK", "Işık", true),
            arrayOf("ışık", "Isik", true),
            arrayOf("sule", "Şule", true),
            arrayOf("ŞULE", "şule", true),
            arrayOf("gul", "Gül", true),
            arrayOf("gul", "Ayşe Gül Yılmaz", true),
            arrayOf("yilmaz", "Ayşe Gül Yılmaz", true),
            arrayOf("cagri", "Çağrı", true),
            arrayOf("ozturk", "Öztürk", true),
            arrayOf("smail", "İsmail", true),
            // Still a substring match.
            arrayOf("isik", "İsmail", false),
            arrayOf("ismail", "Ahmet", false),
            arrayOf("gule", "Gül", false),
            // Plain ASCII, as with LIKE.
            arrayOf("max", "Max Mustermann", true),
            arrayOf("MUSTER", "Max Mustermann", true),
            arrayOf("mux", "Max Mustermann", false),
            // German and other accented latin letters.
            arrayOf("müller", "Müller", true),
            arrayOf("MÜLLER", "müller", true),
            arrayOf("muller", "Müller", true),
            arrayOf("straße", "Straße", true),
            arrayOf("jose", "José", true),
            arrayOf("JOSÉ", "josé", true),
            arrayOf("francois", "François", true),
            arrayOf("lodz", "Łódź", true),
            arrayOf("stefan", "Ștefan", true),
            arrayOf("soren", "Søren", true),
            // Other scripts: case-insensitive where the script has case, literal otherwise.
            arrayOf("иван", "Иван Петров", true),
            arrayOf("ПЕТР", "Иван Петров", true),
            arrayOf("οδυσ", "ΟΔΥΣΣΕΑΣ", true),
            arrayOf("张三", "张三丰", true),
            arrayOf("李四", "张三丰", false),
            arrayOf("किरण", "किरण कुमार", true),
            arrayOf("كريم", "كريم", true),
            // GLOB wildcards in the query are literal.
            arrayOf("a*b", "a*b", true),
            arrayOf("a*b", "axxb", false),
            arrayOf("a?b", "axb", false),
            arrayOf("[a]", "[a]", true),
            arrayOf("[a]", "a", false),
            arrayOf("50%", "50% off", true),
            arrayOf("a_b", "axb", false),
        )
    }
}

class ContactNamePatternVariantsTest {
    @Test
    fun turkishIVariants() {
        val expected = setOf('i', 'I', 'ı', 'İ').map { it.code }
        for (c in expected) {
            Assert.assertTrue(
                "variants of ${String(Character.toChars(c))}",
                ContactNamePattern.variantsOf(c).containsAll(expected),
            )
        }
    }

    @Test
    fun digitsAndPunctuationAreLiteral() {
        for (c in "09 -+.'*?[") {
            Assert.assertEquals(setOf(c.code), ContactNamePattern.variantsOf(c.code))
        }
    }

    @Test
    fun patternShape() {
        Assert.assertEquals("*12[*]3*", ContactNamePattern.glob("12*3"))
    }
}

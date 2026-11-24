package de.mm20.launcher2.contacts.providers

import java.text.Normalizer

/**
 * Builds SQLite `GLOB` patterns that find a query in contact names, ignoring case and accents.
 *
 * SQLite's `LIKE` only folds the case of ASCII letters, so it never matched "ismail" against
 * "İsmail", "isik" against "Işık" or "sule" against "Şule". `GLOB` is case-sensitive, but it
 * supports character classes, so every letter of the query is turned into a class of all its case
 * and accent variants instead (`i` → `[iIıİìíîï…]`). The pattern is still a single substring match
 * evaluated by SQLite, so the prefilter stays as cheap as the old `LIKE`.
 */
internal object ContactNamePattern {

    /**
     * A `GLOB` pattern that matches any string containing [query], ignoring case and accents.
     */
    fun glob(query: String): String {
        val pattern = StringBuilder(query.length * 8 + 2)
        pattern.append('*')
        var i = 0
        while (i < query.length) {
            val codePoint = query.codePointAt(i)
            i += Character.charCount(codePoint)
            val variants = variantsOf(codePoint)
            if (variants.size > 1) {
                pattern.append('[')
                for (variant in variants) pattern.appendCodePoint(variant)
                pattern.append(']')
            } else if (codePoint == '*'.code || codePoint == '?'.code || codePoint == '['.code) {
                // GLOB has no escape character, but a single-character class is literal.
                pattern.append('[').appendCodePoint(codePoint).append(']')
            } else {
                pattern.appendCodePoint(codePoint)
            }
        }
        pattern.append('*')
        return pattern.toString()
    }

    /**
     * All characters that [codePoint] should match: the character itself, its case variants, and,
     * for Latin letters, every accented form of the same base letter.
     */
    internal fun variantsOf(codePoint: Int): Set<Int> {
        if (!Character.isLetter(codePoint)) return setOf(codePoint)
        val base = baseLetter(codePoint)
        latinVariants[base]?.let { return it }
        return caseVariants(codePoint)
    }

    /**
     * The lowercase base letter of [codePoint], without accents. The Turkish dotted and dotless i
     * both map to `i`.
     */
    private fun baseLetter(codePoint: Int): Int {
        extraBaseLetters[codePoint]?.let { return it }
        val lower = Character.toLowerCase(codePoint)
        extraBaseLetters[lower]?.let { return it }
        val decomposed = Normalizer.normalize(String(Character.toChars(lower)), Normalizer.Form.NFD)
        return Character.toLowerCase(decomposed.codePointAt(0))
    }

    private fun caseVariants(codePoint: Int): Set<Int> = linkedSetOf(
        codePoint,
        Character.toLowerCase(codePoint),
        Character.toUpperCase(codePoint),
        Character.toTitleCase(codePoint),
    )

    /**
     * Letters that are commonly typed as a plain ASCII letter, but do not decompose into one.
     */
    private val extraBaseLetters = mapOf(
        'ı'.code to 'i'.code,
        'ł'.code to 'l'.code,
        'đ'.code to 'd'.code,
        'ø'.code to 'o'.code,
    )

    /**
     * Maps each lowercase ASCII letter to itself and all its case and accent variants from the
     * Latin-1 Supplement and Latin Extended-A blocks, plus Romanian ș and ț. This covers the
     * accented letters of Turkish and most European languages.
     */
    private val latinVariants: Map<Int, Set<Int>> = run {
        val map = mutableMapOf<Int, MutableSet<Int>>()
        for (letter in 'a'..'z') {
            map[letter.code] = linkedSetOf(letter.code, letter.uppercaseChar().code)
        }
        val codePoints = (0x00C0..0x017F) + (0x0218..0x021B)
        for (codePoint in codePoints) {
            if (!Character.isLetter(codePoint)) continue
            val variants = map[baseLetter(codePoint)] ?: continue
            variants.addAll(caseVariants(codePoint))
        }
        // Defensive: only keep case variants that fold back to the same base letter.
        map.mapValues { (base, variants) ->
            variants.filterTo(linkedSetOf()) { baseLetter(it) == base }
        }
    }
}

package de.mm20.launcher2.data

import de.mm20.launcher2.search.StringNormalizer
import org.apache.commons.lang3.StringUtils
import java.util.Locale

/**
 * Pre Android 10 StringNormalizer. Only strips accents from latin characters
 */
internal class CompatStringNormalizer: StringNormalizer  {

    override val id: String = "null"

    override fun normalize(input: String): String {
        return compatNormalize(input)
    }

    companion object {
        /**
         * Lowercases [input] and strips accents from latin characters.
         *
         * This is locale-independent on purpose: under a Turkish locale, "I" would lowercase to a
         * dotless "ı", which [StringUtils.stripAccents] keeps, so "Ismail" would no longer match
         * "ismail". The dotless "ı" is folded to "i" for the same reason.
         */
        internal fun compatNormalize(input: String): String {
            return StringUtils.stripAccents(input.lowercase(Locale.ROOT))
                .replace("ı", "i")
                .replace("æ", "ae")
                .replace("œ", "oe")
                .replace("ß", "ss")
        }
    }
}

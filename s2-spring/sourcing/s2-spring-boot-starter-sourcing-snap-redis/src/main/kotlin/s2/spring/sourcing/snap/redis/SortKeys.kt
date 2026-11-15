package s2.spring.sourcing.snap.redis

import java.text.Collator
import java.util.HexFormat
import java.util.Locale
import tools.jackson.databind.node.ObjectNode

/**
 * The sort keys a view writes into each stored document, one per text attribute it sorts: the store sorts the key,
 * never the text. A key is the text's collation key (root locale, accents then case after the letters), in hex so
 * that its byte order is the collation order whatever the store normalises, then the snapshot's id, so that equal
 * texts take one order on every page.
 */
internal class SortKeys(private val sorted: List<Sorted>) {

    data class Sorted(val attribute: String, val pointer: String, val key: String)

    private val keyOf = sorted.associate { it.attribute to it.key }

    fun sortAttribute(attribute: String): String = keyOf[attribute] ?: attribute

    /** A text [document] lacks gets no key, and sorts where the store puts a missing value. */
    fun write(document: ObjectNode, id: String) {
        val keys = document.objectNode()
        sorted.forEach { field ->
            val text = document.at(field.pointer)
            if (text.isString) keys.put(field.attribute, key(text = text.stringValue(), id = id))
        }
        if (!keys.isEmpty) document.set(DOCUMENT_FIELD, keys)
    }

    companion object {
        const val DOCUMENT_FIELD = "__s2_sort"

        private const val ATTRIBUTE_PREFIX = "__s2_sort_"

        // Below every hex digit: a key that starts another sorts first, and the id only breaks ties.
        private const val ID_SEPARATOR = " "

        private val PLAIN_PATH = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")

        private val collator: Collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.TERTIARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }

        /** Null when [path] is not a plain dotted name: an array member has no single text to sort by. */
        fun sorted(attribute: String, path: String): Sorted? = path.takeIf(PLAIN_PATH::matches)?.let { plain ->
            Sorted(
                attribute = attribute,
                pointer = plain.split('.').joinToString(separator = "/", prefix = "/"),
                key = ATTRIBUTE_PREFIX + attribute,
            )
        }

        fun path(sorted: Sorted): String = "$.$DOCUMENT_FIELD[\"${sorted.attribute}\"]"

        // A collator is shared, and RuleBasedCollator synchronises getCollationKey.
        fun key(text: String, id: String): String =
            HexFormat.of().formatHex(collator.getCollationKey(text).toByteArray()) + ID_SEPARATOR + id
    }
}

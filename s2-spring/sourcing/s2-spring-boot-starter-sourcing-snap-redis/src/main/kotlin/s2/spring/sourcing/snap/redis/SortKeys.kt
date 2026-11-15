package s2.spring.sourcing.snap.redis

import java.text.Collator
import java.util.HexFormat
import java.util.Locale
import tools.jackson.databind.node.ObjectNode

/**
 * The sort keys a view writes into each stored document, one per text attribute it sorts: the store sorts the key,
 * never the text. A key is the text's collation key (root locale, accents then case after the letters) over its first
 * [SORTED_LENGTH] characters, in hex so that its byte order is the collation order whatever the store normalises,
 * then the snapshot's id, so that equal texts take one order on every page. A missing text sorts after every text.
 */
internal class SortKeys(private val sorted: List<Sorted>) : DerivedField {

    data class Sorted(val attribute: String, val pointer: String, val key: String)

    private val keyOf = sorted.associate { it.attribute to it.key }

    override val documentField: String get() = DOCUMENT_FIELD

    override val isEmpty: Boolean get() = sorted.isEmpty()

    fun sortAttribute(attribute: String): String = keyOf[attribute] ?: attribute

    /** The keys of [document]; a text that is not a string is missing. */
    fun keysOf(document: ObjectNode, id: String): ObjectNode = document.objectNode().apply {
        sorted.forEach { field ->
            val text = document.at(field.pointer)
            put(field.attribute, if (text.isString) key(text = text.stringValue(), id = id) else missing(id))
        }
    }

    override fun valueOf(document: ObjectNode, id: String): ObjectNode = keysOf(document = document, id = id)

    companion object {
        const val DOCUMENT_FIELD = "__s2_sort"

        /** Characters of a text its key reads: texts equal that far sort by id. */
        const val SORTED_LENGTH = 100

        private const val ATTRIBUTE_PREFIX = "__s2_sort_"

        // Below every hex digit: a key that starts another sorts first, and the id only breaks ties.
        private const val ID_SEPARATOR = " "

        // Above every hex digit: a missing text sorts last ascending, first descending, on every Redis Stack.
        private const val MISSING = "~"

        private val PLAIN = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")

        private val collator: Collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.TERTIARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }

        /**
         * Null when [path] or [attribute] is not a plain dotted name of letters, digits and underscores: a key's JSON
         * path and attribute are built from both, and an array member has no single text to sort by.
         */
        fun sorted(attribute: String, path: String): Sorted? =
            if (PLAIN.matches(path) && PLAIN.matches(attribute)) {
                Sorted(
                    attribute = attribute,
                    pointer = path.split('.').joinToString(separator = "/", prefix = "/"),
                    key = ATTRIBUTE_PREFIX + attribute,
                )
            } else {
                null
            }

        fun path(sorted: Sorted): String = "$.$DOCUMENT_FIELD[\"${sorted.attribute}\"]"

        fun key(text: String, id: String): String = collationHex(text) + ID_SEPARATOR + id

        fun missing(id: String): String = MISSING + ID_SEPARATOR + id

        // A collator is shared, and RuleBasedCollator synchronises getCollationKey.
        fun collationHex(text: String): String {
            val head = buildString { text.codePoints().limit(SORTED_LENGTH.toLong()).forEach(::appendCodePoint) }
            return HexFormat.of().formatHex(collator.getCollationKey(head).toByteArray())
        }
    }
}

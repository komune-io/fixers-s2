package s2.spring.sourcing.snap.redis

import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.ComplexData
import io.lettuce.core.protocol.CommandArgs
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.HexFormat

/**
 * An index as `FT.CREATE` makes it: the keys `<index>-…` it reads — the dash keeps out a model whose name starts
 * with this one's — every word kept, stopwords included, and per attribute its JSON path, name, type and flags.
 * Its [fingerprint] names all of it, the phonetic matcher included, which `FT.INFO` does not show. So that the index
 * itself shows all of it, the fingerprint's digest is the name of one more attribute, [marker], on a path no
 * document holds: [isShownBy] then reads the whole definition — prefix, attributes, marker, language, stopwords —
 * from `FT.INFO` alone, with nothing written beside the index that a concurrent writer could leave out of step.
 */
internal class IndexDefinition private constructor(
    private val arguments: List<String>,
    private val prefix: String,
    private val attributes: List<Attribute>,
) {
    /** One attribute as `FT.INFO` shows it: its JSON path, the name queries use, and its type. */
    data class Attribute(val identifier: String, val name: String, val type: String)

    /** The `FT.CREATE` arguments that make the index, its [marker] aside. */
    val fingerprint: String get() = arguments.joinToString(" ")

    /** The attribute name that carries [fingerprint]'s digest into `FT.INFO`. */
    val marker: String get() = MARKER_PREFIX + digest(fingerprint)

    fun createArgs(indexName: String): CommandArgs<String, String> =
        (arguments + listOf(MARKER_PATH, "AS", marker, MARKER_TYPE))
            .fold(CommandArgs(StringCodec.UTF8).add(indexName)) { args, argument -> args.add(argument) }

    /**
     * Whether the index [info] describes reads what this definition reads: its one prefix, its attributes in order
     * then its [marker], RediSearch's default language and no stopwords. A reply that cannot be read that way fails.
     */
    fun isShownBy(info: Map<String, Any?>): Boolean {
        val definition = checkNotNull(asFields(info["index_definition"])) { "FT.INFO carries no index_definition" }
        val read = definition["prefixes"]
        val prefixes = if (read is List<*>) read.map { it?.toString() } else listOf(read?.toString())
        val shown = checkNotNull(info["attributes"] as? List<*>) { "FT.INFO carries no attributes" }.map(::attribute)
        val stopwords = info["stopwords_list"]
        // FT.INFO leaves RediSearch's own default language out of its answer.
        val language = definition["default_language"]?.toString() ?: DEFAULT_LANGUAGE
        val expected = attributes + Attribute(identifier = MARKER_PATH, name = marker, type = MARKER_TYPE)
        return prefixes == listOf(prefix) && shown == expected && language == DEFAULT_LANGUAGE &&
            stopwords is List<*> && stopwords.isEmpty()
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "english"
        private const val MARKER_PATH = "$.__s2_index_definition"
        private const val MARKER_PREFIX = "__s2_definition_"
        private const val MARKER_TYPE = "TAG"
        private const val MARKER_DIGEST_LENGTH = 16
        private val HEAD = listOf("identifier", "attribute", "type")

        fun from(indexName: String, fields: List<RedisIndexField>, settings: IndexSettings): IndexDefinition {
            require(fields.isNotEmpty()) { "Index[$indexName] needs at least one field" }
            val names = fields.map { it.name ?: it.field }
            require(names.distinct().size == names.size) { "Index[$indexName] names a field twice: $names" }
            val prefix = "$indexName-"
            // RediSearch's default list leaves English words out of the index, so a name holding « Or » or « If »
            // could not be found by that word.
            val arguments = mutableListOf("ON", "JSON", "PREFIX", "1", prefix, "STOPWORDS", "0", "SCHEMA")
            fields.forEach { field ->
                arguments += listOf("$.${field.field}", "AS", field.name ?: field.field, field.type.name)
                arguments += when (field.type) {
                    RedisFieldType.TEXT -> listOf("NOSTEM") +
                        settings.phonetic?.let { listOf("PHONETIC", it.value) }.orEmpty() +
                        "SORTABLE"
                    RedisFieldType.NUMERIC -> listOf("SORTABLE")
                    RedisFieldType.GEO, RedisFieldType.TAG -> emptyList()
                }
            }
            val attributes = fields.map {
                Attribute(identifier = "$.${it.field}", name = it.name ?: it.field, type = it.type.name)
            }
            return IndexDefinition(arguments = arguments, prefix = prefix, attributes = attributes)
        }

        // A digest short enough for an attribute name; 64 bits tell apart every definition one index will ever have.
        private fun digest(text: String): String =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)))
                .take(MARKER_DIGEST_LENGTH)

        // RESP3 names each value; RESP2 lists `identifier`, `attribute`, `type` at 0, 2, 4, their values at 1, 3, 5.
        private fun attribute(entry: Any?): Attribute {
            val head = when (entry) {
                is Map<*, *> -> HEAD.map { entry[it] }
                is List<*> -> HEAD.mapIndexed { i, key ->
                    entry.getOrNull(2 * i + 1).takeIf { entry.getOrNull(2 * i) == key }
                }
                else -> null
            }
            val read = head?.run { map { it?.toString() }.takeIf { it.all { value -> value != null } } }
            val values = checkNotNull(read) {
                "FT.INFO attribute unreadable: ${entry ?: "none"}"
            }
            return Attribute(identifier = values[0].orEmpty(), name = values[1].orEmpty(), type = values[2].orEmpty())
        }
    }
}

/** A reply decoded into plain maps, lists and scalars. */
internal fun plain(value: Any?): Any? = when (value) {
    is ComplexData -> when {
        value.isMap -> value.dynamicMap.entries.associate { (key, item) ->
            checkNotNull(plain(key)) { NULL_KEY }.toString() to plain(item)
        }
        value.isSet -> value.dynamicSet.map(::plain)
        else -> value.dynamicList.map(::plain)
    }
    is ByteBuffer -> StringCodec.UTF8.decodeValue(value)
    else -> value
}

/** A map as RESP3 sends it, or the flat key-value list RESP2 sends for the same thing; null for anything else. */
internal fun asFields(value: Any?): Map<String, Any?>? = when (value) {
    is Map<*, *> -> value.entries.associate { (key, item) -> checkNotNull(key) { NULL_KEY }.toString() to item }
    is List<*> -> if (value.size % 2 == 0) {
        value.chunked(2).associate { (key, item) -> checkNotNull(key) { NULL_KEY }.toString() to item }
    } else {
        null
    }
    else -> null
}

private const val NULL_KEY = "A reply map carries a null key"

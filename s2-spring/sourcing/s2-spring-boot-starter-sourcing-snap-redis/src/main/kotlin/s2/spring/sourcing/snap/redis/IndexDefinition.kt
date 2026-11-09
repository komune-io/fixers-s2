package s2.spring.sourcing.snap.redis

import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.ComplexData
import io.lettuce.core.protocol.CommandArgs
import java.nio.ByteBuffer

/**
 * What an index is, in the terms `FT.INFO` reads back: the key prefixes it reads, its language, whether it
 * ignores stopwords, and per attribute, in schema order, its path, name, type and the flags this module sets.
 * Two definitions are equal when the index needs no re-creation. The [phonetic] matcher is sent on every TEXT
 * attribute and not compared: `FT.INFO` does not show it (Redis Stack 6.2.2), so an index cannot tell it.
 */
data class IndexDefinition(
	val prefixes: List<String>,
	val language: String,
	val keepsStopwords: Boolean,
	val attributes: List<Attribute>,
	val phonetic: String?,
) {
	data class Attribute(val identifier: String, val name: String, val type: String, val flags: Set<String>)

	override fun equals(other: Any?): Boolean =
		other is IndexDefinition &&
			prefixes == other.prefixes &&
			language == other.language &&
			keepsStopwords == other.keepsStopwords &&
			attributes == other.attributes

	override fun hashCode(): Int = listOf(prefixes, language, keepsStopwords, attributes).hashCode()

	fun createArgs(indexName: String): CommandArgs<String, String> {
		val args = CommandArgs(StringCodec.UTF8)
			.add(indexName)
			.add("ON").add("JSON")
			.add("PREFIX").add(prefixes.size.toString())
		prefixes.forEach { args.add(it) }
		args.add("LANGUAGE").add(language)
		// RediSearch's default list leaves English words out of the index, so a name holding « Or » or « If »
		// could not be found by that word, and a search naming it found nothing.
		if (!keepsStopwords) args.add("STOPWORDS").add("0")
		args.add("SCHEMA")
		attributes.forEach { attribute ->
			args.add(attribute.identifier).add("AS").add(attribute.name).add(attribute.type)
			if (NOSTEM in attribute.flags) args.add(NOSTEM)
			if (attribute.type == TEXT && phonetic != null) args.add("PHONETIC").add(phonetic)
			if (SORTABLE in attribute.flags) args.add(SORTABLE)
		}
		return args
	}

	companion object {
		private val DEFAULT_LANGUAGE = IndexLanguage.ENGLISH.value
		private const val TEXT = "TEXT"
		private const val NOSTEM = "NOSTEM"
		private const val SORTABLE = "SORTABLE"

		/** The flags each type is created with, and the only ones compared. */
		private val FLAGS = mapOf(TEXT to setOf(NOSTEM, SORTABLE), "NUMERIC" to setOf(SORTABLE))
		private val COMPARED_FLAGS = FLAGS.values.flatten().toSet()

		/**
		 * The index of [indexName] over [fields]: it reads the keys `<indexName>-…` only, the dash keeping out
		 * a model whose name starts with this one's.
		 */
		fun of(indexName: String, fields: List<RedisIndexField>, settings: IndexSettings) = IndexDefinition(
			prefixes = listOf("$indexName-"),
			language = settings.language.value,
			keepsStopwords = false,
			attributes = fields.map { field ->
				val type = when (field.type) {
					RedisFieldType.TEXT -> TEXT
					RedisFieldType.GEO -> "GEO"
					RedisFieldType.NUMERIC -> "NUMERIC"
					RedisFieldType.TAG -> "TAG"
				}
				Attribute("$.${field.field}", field.name ?: field.field, type, FLAGS[type].orEmpty())
			},
			phonetic = settings.phonetic?.value,
		)

		/**
		 * The definition an `FT.INFO` answer describes, whichever protocol shaped it; null when the answer
		 * cannot be read as one, which says nothing about the index itself.
		 */
		fun read(info: Map<String, Any?>): IndexDefinition? {
			val definition = asFields(info["index_definition"])
			val prefixes = definition?.get("prefixes")?.let { read ->
				if (read is List<*>) read.map(Any?::toString) else listOf(read.toString())
			}
			// One unreadable attribute makes the whole answer unreadable.
			val attributes = (info["attributes"] as? List<*>)?.map(::attribute)?.takeIf { null !in it }?.filterNotNull()
			if (prefixes == null || attributes == null) return null
			val stopwords = info["stopwords_list"]
			return IndexDefinition(
				prefixes = prefixes,
				// `FT.INFO` leaves RediSearch's own default language out of its answer.
				language = definition["default_language"]?.toString() ?: DEFAULT_LANGUAGE,
				// Without a list of its own the index keeps RediSearch's default one.
				keepsStopwords = !(stopwords is List<*> && stopwords.isEmpty()),
				attributes = attributes,
				phonetic = null,
			)
		}

		private val HEAD = listOf("identifier", "attribute", "type")

		/** RESP3 names each value; RESP2 lists `identifier`, `attribute`, `type` at 0, 2, 4, their values at 1, 3, 5. */
		private fun attribute(entry: Any?): Attribute? {
			val head = when (entry) {
				is Map<*, *> -> HEAD.map { entry[it] }
				is List<*> -> HEAD.mapIndexed { i, key -> entry.getOrNull(2 * i + 1).takeIf { entry.getOrNull(2 * i) == key } }
				else -> null
			}
			return head?.takeIf { values -> values.all { it != null } }?.let { (identifier, name, type) ->
				val flags = leaves(entry).filter { it in COMPARED_FLAGS }.toSet()
				Attribute(identifier.toString(), name.toString(), type.toString(), flags)
			}
		}
	}
}

/** A reply decoded into plain maps, lists and scalars. */
internal fun plain(value: Any?): Any? = when (value) {
	is ComplexData -> when {
		value.isMap -> value.dynamicMap.entries.associate { (key, item) -> plain(key).toString() to plain(item) }
		value.isSet -> value.dynamicSet.map(::plain)
		else -> value.dynamicList.map(::plain)
	}
	is ByteBuffer -> StringCodec.UTF8.decodeValue(value)
	else -> value
}

/** A map as RESP3 sends it, or the flat key-value list RESP2 sends for the same thing. */
internal fun asFields(value: Any?): Map<String, Any?>? = when (value) {
	is Map<*, *> -> value.entries.associate { (key, item) -> key.toString() to item }
	is List<*> -> if (value.size % 2 == 0) {
		value.chunked(2).associate { (key, item) -> key.toString() to item }
	} else {
		null
	}
	else -> null
}

private fun leaves(value: Any?): List<String> = when (value) {
	is Map<*, *> -> value.values.flatMap(::leaves)
	is List<*> -> value.flatMap(::leaves)
	null -> emptyList()
	else -> listOf(value.toString())
}

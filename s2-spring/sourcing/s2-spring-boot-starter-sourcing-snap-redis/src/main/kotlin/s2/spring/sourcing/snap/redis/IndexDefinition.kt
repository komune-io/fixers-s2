package s2.spring.sourcing.snap.redis

import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.ComplexData
import io.lettuce.core.protocol.CommandArgs
import java.nio.ByteBuffer

/**
 * What an index is, in the terms `FT.INFO` reads back: its language, whether it ignores stopwords, and per
 * attribute, in schema order, its path, name, type and the flags this module sets. Two definitions are
 * equal when the index needs no re-creation. The [phonetic] matcher, which `FT.INFO` does not show, is sent
 * on every TEXT attribute and not compared.
 */
data class IndexDefinition(
	val language: String,
	val keepsStopwords: Boolean,
	val attributes: List<Attribute>,
	val phonetic: String? = null,
) {
	data class Attribute(val identifier: String, val name: String, val type: String, val flags: Set<String>)

	override fun equals(other: Any?): Boolean =
		other is IndexDefinition &&
			language == other.language &&
			keepsStopwords == other.keepsStopwords &&
			attributes == other.attributes

	override fun hashCode(): Int = listOf(language, keepsStopwords, attributes).hashCode()

	fun createArgs(indexName: String): CommandArgs<String, String> {
		val args = CommandArgs(StringCodec.UTF8)
			.add(indexName)
			.add("ON").add("JSON")
			.add("PREFIX").add("1").add(indexName)
			.add("LANGUAGE").add(language)
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

		fun of(fields: List<RedisIndexField>, settings: IndexSettings) = IndexDefinition(
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
			// `FT.INFO` leaves RediSearch's own default language out of its answer.
			val language = asFields(info["index_definition"])?.let { it["default_language"]?.toString() ?: DEFAULT_LANGUAGE }
			// One unreadable attribute makes the whole answer unreadable.
			val attributes = (info["attributes"] as? List<*>)?.map(::attribute)?.takeIf { null !in it }?.filterNotNull()
			if (language == null || attributes == null) return null
			val stopwords = info["stopwords_list"]
			// Without a list of its own the index keeps RediSearch's default one.
			return IndexDefinition(language, !(stopwords is List<*> && stopwords.isEmpty()), attributes)
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

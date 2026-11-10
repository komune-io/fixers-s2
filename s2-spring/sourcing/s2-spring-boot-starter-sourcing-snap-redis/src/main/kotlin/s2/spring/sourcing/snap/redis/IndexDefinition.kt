package s2.spring.sourcing.snap.redis

import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.ComplexData
import io.lettuce.core.protocol.CommandArgs
import java.nio.ByteBuffer

/**
 * An index as `FT.CREATE` makes it: the keys `<index>-…` it reads — the dash keeps out a model whose name starts
 * with this one's — every word kept, stopwords included, and per attribute its JSON path, name, type and flags.
 * Its [fingerprint] names all of it, the phonetic matcher included, which `FT.INFO` does not show.
 */
internal class IndexDefinition private constructor(private val arguments: List<String>) {

	val fingerprint: String get() = arguments.joinToString(" ")

	fun createArgs(indexName: String): CommandArgs<String, String> =
		arguments.fold(CommandArgs(StringCodec.UTF8).add(indexName)) { args, argument -> args.add(argument) }

	companion object {
		fun of(indexName: String, fields: List<RedisIndexField>, settings: IndexSettings): IndexDefinition {
			require(fields.isNotEmpty()) { "Index[$indexName] needs at least one field" }
			val names = fields.map { it.name ?: it.field }
			require(names.distinct().size == names.size) { "Index[$indexName] names a field twice: $names" }
			// RediSearch's default list leaves English words out of the index, so a name holding « Or » or « If »
			// could not be found by that word.
			val arguments = mutableListOf("ON", "JSON", "PREFIX", "1", "$indexName-", "STOPWORDS", "0", "SCHEMA")
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
			return IndexDefinition(arguments)
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

package s2.spring.sourcing.snap.redis

enum class RedisFieldType {
	TEXT, GEO, NUMERIC, TAG
}

/** One attribute of a view's index: the JSON [field] it reads, its [type], and the [name] queries use. */
class RedisIndexField(
	val field: String,
	val type: RedisFieldType,
	val name: String? = null,
) {
	companion object {
		/**
		 * A field holding a JSON array of scalars, indexed member by member.
		 *
		 * RediSearch refuses a document whose TAG or TEXT path resolves to an array: it drops
		 * **the whole document** from the index and only counts it in `hash_indexing_failures`
		 * — no error, no log, and `createIndex` would not see it anyway since `FT.CREATE`
		 * itself succeeds. The path must therefore walk into the array with `[*]`, while the
		 * attribute keeps the plain field name that [tagClause] queries.
		 */
		fun array(field: String, type: RedisFieldType) = RedisIndexField("$field[*]", type, name = field)
	}
}

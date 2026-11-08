package s2.dsl.automate.extention

/**
 * A Kotlin/JS enum's own fields are mangled, so its JSON copy is rewritten as
 * `{ name, ordinal, position }`; only that shape is read, not any object with a `name`.
 */
internal actual fun enumCopyName(value: Any): String? {
	val copy = value.asDynamic()
	val name = copy.name
	return if (name is String && copy.ordinal is Number) name else null
}

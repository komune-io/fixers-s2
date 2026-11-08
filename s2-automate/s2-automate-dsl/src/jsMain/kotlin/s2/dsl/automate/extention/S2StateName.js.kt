package s2.dsl.automate.extention

import s2.dsl.automate.S2State
import s2.dsl.automate.stateName

/**
 * Also reads a `{ name }` object: a Kotlin/JS enum's own fields are mangled, so a JSON copy of
 * one has to be rewritten into that shape to be readable at all.
 */
actual fun s2StateName(status: Any?): String? = when (status) {
	null -> null
	is String -> status
	is S2State -> status.stateName()
	else -> status.asDynamic().name as? String
}

package s2.dsl.automate.extention

import s2.dsl.automate.S2State
import s2.dsl.automate.stateName

/**
 * The JVM holds the state itself, or the name a deserializer has not turned back into one.
 */
actual fun s2StateName(status: Any?): String? = when (status) {
	is String -> status
	is S2State -> status.stateName()
	else -> null
}

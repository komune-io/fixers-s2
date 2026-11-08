package s2.dsl.automate.extention

import s2.dsl.automate.S2State
import s2.dsl.automate.stateName

actual fun s2StateName(status: Any?): String? = when (status) {
	is String -> status
	is S2State -> status.stateName()
	else -> null
}

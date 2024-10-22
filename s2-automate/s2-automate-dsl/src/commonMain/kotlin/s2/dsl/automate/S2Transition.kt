package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@JsExport
@JsName("S2InitTransition")
open class S2InitTransition(
	open val to: S2StateValue,
	open val role: S2Role,
	open val action: S2TransitionValue,
)

@Serializable
@JsExport
@JsName("S2Transition")
open class S2Transition(
	open val from: S2StateValue?,
	open val to: S2StateValue,
	open val role: S2RoleValue,
	open val action: S2TransitionValue,
	open val result: S2TransitionValue?,
)

@JsExport
@Serializable
class S2TransitionValue(
	val name: String,
)
@JsExport
@Serializable
class S2RoleValue(
	val name: String,
)
@JsExport
@Serializable
class S2StateValue(
	val name: String,
	val position: Int
)


fun KClass<out Msg>.toValue(): S2TransitionValue {
	return S2TransitionValue(
		name = this.simpleName!!,
	)
}

fun S2Role.toValue(): S2RoleValue {
	return S2RoleValue(
		name = this::class.simpleName!!,
	)
}
fun S2State.toValue(): S2StateValue {
	return S2StateValue(
		name = this::class.simpleName!!,
		position = this.position,
	)
}

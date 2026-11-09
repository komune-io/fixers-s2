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
		name = s2SimpleName(),
	)
}

fun S2Role.toValue(): S2RoleValue {
	return S2RoleValue(
		name = s2Name(),
	)
}
fun S2State.toValue(): S2StateValue {
	return S2StateValue(
		name = s2Name(),
		position = this.position,
	)
}

/**
 * The name the JSON carries for a state or a role. An enum entry serializes as its own name, which
 * `this::class` does not give: it names the enum, or nothing for an entry with a body.
 */
internal fun Any.s2Name(): String = (this as? Enum<*>)?.name ?: this::class.s2SimpleName()

/** The simple name S2 serializes for a class; an anonymous class has none and cannot be used. */
@PublishedApi
internal fun KClass<*>.s2SimpleName(): String = requireNotNull(simpleName) { "An S2 class needs a name: $this" }

/** Whether [type] names this transition's command or its resulting event. */
internal fun S2Transition.isNamed(type: String): Boolean = result?.name == type || action.name == type

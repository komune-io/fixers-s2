package s2.dsl.automate

import kotlin.reflect.KClass

open class S2SubMachine(
    open val automate: S2Automate,
    open val startsOn: List<KClass<out Msg>> = emptyList(),
    open val endsOn: List<KClass<out Msg>> = emptyList(),
    open val autostart: Boolean = false,
    open val blocking: Boolean = false,
    open val singleton: Boolean = false
)

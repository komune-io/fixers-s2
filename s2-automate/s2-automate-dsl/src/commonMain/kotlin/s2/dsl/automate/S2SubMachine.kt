package s2.dsl.automate

import kotlin.reflect.KClass

open class S2SubMachine(
    open val automate: S2Automate,
    open val startsOn: List<KClass<out Msg>> = emptyList(),
    open val endsOn: List<KClass<out Msg>> = emptyList(),
    open val isAutostart: Boolean = false,
    open val isBlocking: Boolean = false,
    open val isSingleton: Boolean = false
)

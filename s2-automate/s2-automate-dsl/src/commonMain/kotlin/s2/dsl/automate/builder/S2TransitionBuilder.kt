package s2.dsl.automate.builder

import kotlin.properties.Delegates
import kotlin.reflect.KClass
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State

class S2TransitionBuilder {
    var from: S2State? = null
    val froms = mutableListOf<S2State>()
    var to: S2State by Delegates.notNull()
    var role: S2Role by Delegates.notNull()
    var evt: KClass<out Evt>? = null
}

class S2NodeTransitionBuilder {
    var to: S2State? = null
    var role: S2Role by Delegates.notNull()
    var evt: KClass<out Evt>? = null
}

class S2SelfTransitionBuilder {
    val states = mutableListOf<S2State>()
    var role: S2Role by Delegates.notNull()
    var evt: KClass<out Evt>? = null
}

class S2InitTransitionBuilder {
    var to: S2State by Delegates.notNull()
    var role: S2Role by Delegates.notNull()
    var evt: KClass<out Evt>? = null
}

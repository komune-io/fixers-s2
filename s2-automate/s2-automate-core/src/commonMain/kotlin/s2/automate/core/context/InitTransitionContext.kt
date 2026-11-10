package s2.automate.core.context

import s2.dsl.automate.Msg

data class InitTransitionContext<AUTOMATE>(
    val automateContext: AutomateContext<AUTOMATE>,
    val msg: Msg,
)

package s2.automate.core.context

import f2.dsl.fnc.operators.Batch
import s2.automate.core.config.S2BatchProperties


class AutomateContext<AUTOMATE>(
	val automate: AUTOMATE,
	val batch: S2BatchProperties
)

fun S2BatchProperties.asBatch() = Batch(
	size = size,
	concurrency = concurrency
)

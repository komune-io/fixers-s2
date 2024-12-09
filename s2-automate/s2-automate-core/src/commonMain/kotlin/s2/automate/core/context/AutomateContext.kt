package s2.automate.core.context

import f2.dsl.fnc.operators.Batch
import s2.automate.core.engine.BatchProperties


class AutomateContext<AUTOMATE>(
	val automate: AUTOMATE,
	val batch: BatchProperties
)

fun BatchProperties.asBatch() = Batch(
	size = size,
	concurrency = concurrency
)

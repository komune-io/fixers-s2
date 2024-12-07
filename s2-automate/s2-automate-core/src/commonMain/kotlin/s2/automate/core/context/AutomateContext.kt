package s2.automate.core.context

import s2.automate.core.engine.BatchParams

class AutomateContext<AUTOMATE>(
	val automate: AUTOMATE,
	val batch: BatchParams = BatchParams()
)

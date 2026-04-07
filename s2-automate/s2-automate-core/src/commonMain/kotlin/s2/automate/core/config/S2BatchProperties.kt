package s2.automate.core.config

import f2.dsl.fnc.operators.BATCH_DEFAULT_CONCURRENCY
import f2.dsl.fnc.operators.BATCH_DEFAULT_SIZE

data class S2BatchProperties(
    val size: Int = BATCH_DEFAULT_SIZE,
    val concurrency: Int = BATCH_DEFAULT_CONCURRENCY,
)

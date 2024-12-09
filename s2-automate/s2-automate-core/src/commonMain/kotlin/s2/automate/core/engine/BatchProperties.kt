package s2.automate.core.engine

import f2.dsl.fnc.operators.BATCH_DEFAULT_SIZE
import f2.dsl.fnc.operators.BATCH_DEFAULT_CONCURRENCY

data class BatchProperties(
    val size: Int = BATCH_DEFAULT_SIZE,
    val concurrency: Int = BATCH_DEFAULT_CONCURRENCY,
)

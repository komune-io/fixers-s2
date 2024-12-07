package s2.automate.core.engine

import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import ssm.chaincode.dsl.config.InvokeChunkedProps

data class BatchParams(
    val chunk: InvokeChunkedProps = InvokeChunkedProps(),
    val concurrency: Int = DEFAULT_CONCURRENCY,
)

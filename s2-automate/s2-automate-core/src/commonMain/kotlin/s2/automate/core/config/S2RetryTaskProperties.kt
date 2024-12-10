package s2.automate.core.config

data class S2RetryTaskProperties(
    val maxAttempts: Int = 5,
    val delayMillis: Long = 1000,
)

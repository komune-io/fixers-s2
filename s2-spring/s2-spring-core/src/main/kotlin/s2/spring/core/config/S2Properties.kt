package s2.spring.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.config.S2RetryTaskProperties


@ConfigurationProperties(prefix = "s2")
class S2Properties(
    val retry: S2RetryTaskProperties?,
    val batch: S2BatchProperties?
)

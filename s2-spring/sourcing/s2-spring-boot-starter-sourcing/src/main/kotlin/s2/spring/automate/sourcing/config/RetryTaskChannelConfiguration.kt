package s2.spring.automate.sourcing.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.OptimisticLockingFailureException
import s2.automate.core.config.S2RetryTaskProperties
import s2.automate.core.storing.snap.RetryTaskChannel
import s2.spring.core.config.S2Properties

@Configuration
@EnableConfigurationProperties(S2Properties::class)
open class RetryTaskChannelConfiguration {

	@Bean
	open fun persistTaskChannel(s2Properties: S2Properties): RetryTaskChannel {
		val retry = s2Properties.retry ?: S2RetryTaskProperties()
		return RetryTaskChannel(
			maxAttempts = retry.maxAttempts,
			delayMillis = retry.delayMillis,
			retryOn = OptimisticLockingFailureException::class)
	}
}

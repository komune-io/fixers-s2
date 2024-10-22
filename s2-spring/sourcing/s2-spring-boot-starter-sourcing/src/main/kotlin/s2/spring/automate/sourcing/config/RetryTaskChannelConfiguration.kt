package s2.spring.automate.sourcing.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.OptimisticLockingFailureException
import s2.automate.core.snap.RetryTaskChannel

@Configuration
open class RetryTaskChannelConfiguration {

	@Bean
	open fun persistTaskChannel(): RetryTaskChannel {
		return RetryTaskChannel(retryOn = OptimisticLockingFailureException::class)
	}
}

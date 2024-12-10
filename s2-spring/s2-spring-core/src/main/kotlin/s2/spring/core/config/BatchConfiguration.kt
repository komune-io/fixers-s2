package s2.spring.core.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import s2.automate.core.config.S2BatchProperties

@Configuration
@EnableConfigurationProperties(S2Properties::class)
open class BatchConfiguration {

	@Bean
	@ConditionalOnMissingBean(S2BatchProperties::class)
	open fun s2BatchProperties(s2Properties: S2Properties): S2BatchProperties =
		s2Properties.batch ?: S2BatchProperties()
}

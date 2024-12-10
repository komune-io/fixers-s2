package s2.spring.sourcing.ssm.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import s2.spring.core.config.S2Properties
import ssm.chaincode.dsl.config.SsmBatchProperties

@Configuration
@EnableConfigurationProperties(S2Properties::class)
open class SsmBatchConfiguration {

	@Bean
	@ConditionalOnMissingBean(SsmBatchProperties::class)
	open fun ssmBatchProperties(s2Properties: S2Properties): SsmBatchProperties? {
		return s2Properties.batch?.let {
			SsmBatchProperties(
				size = it.size,
				concurrency = it.concurrency
			)
		}
	}
}

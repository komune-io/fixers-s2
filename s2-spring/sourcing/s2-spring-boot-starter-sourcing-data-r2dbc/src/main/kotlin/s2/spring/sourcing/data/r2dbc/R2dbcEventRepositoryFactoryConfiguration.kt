package s2.spring.sourcing.data.r2dbc

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient


@Configuration
open class R2dbcEventRepositoryFactoryConfiguration {
	@Bean
	open fun r2dbcRepositoryFactory(
		databaseClient: DatabaseClient, template: R2dbcEntityTemplate
	): R2dbcEventRepositoryFactory {
		return R2dbcEventRepositoryFactory(databaseClient, template)
	}
}

package s2.spring.sourcing.data.r2dbc

import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator


@Configuration
open class R2dbcReactiveRepositoryConfig {
	@Bean
	open fun r2dbcRepositoryFactory(template: R2dbcEntityTemplate): ReactiveRepositoryFactorySupport {
		return R2dbcRepositoryFactory(template)
	}

	@Bean
	open fun initializer(
		@Qualifier("connectionFactory") connectionFactory: ConnectionFactory
	): ConnectionFactoryInitializer {
		val initializer = ConnectionFactoryInitializer()
		initializer.setConnectionFactory(connectionFactory)
		val resource =
			ResourceDatabasePopulator(ClassPathResource("init.sql"))
		initializer.setDatabasePopulator(resource)
		return initializer
	}
}

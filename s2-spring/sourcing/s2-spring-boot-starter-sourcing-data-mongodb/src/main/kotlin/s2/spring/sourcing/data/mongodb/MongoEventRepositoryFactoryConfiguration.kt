package s2.spring.sourcing.data.mongodb

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoOperations


@Configuration
open class MongoEventRepositoryFactoryConfiguration {

	@Bean
	open fun mongoRepositoryFactory(mongoOperations: ReactiveMongoOperations): MongoEventRepositoryFactory {
		return MongoEventRepositoryFactory(mongoOperations)
	}


}

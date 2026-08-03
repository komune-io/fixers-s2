package s2.spring.sourcing.data.mongodb.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MongoDBContainer

@TestConfiguration(proxyBeanMethods = false)
open class ContainersConfig {

    companion object {
        const val MONGO_IMAGE: String = "mongo:7.0"
    }

    @Bean
    @ServiceConnection
    open fun mongo(): MongoDBContainer {
        return MongoDBContainer(MONGO_IMAGE)
    }
}

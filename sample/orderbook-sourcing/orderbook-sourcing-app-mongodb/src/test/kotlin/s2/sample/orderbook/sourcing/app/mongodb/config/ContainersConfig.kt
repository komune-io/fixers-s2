package s2.sample.orderbook.sourcing.app.mongodb.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class ContainersConfig {

    companion object {
        const val MONGO_IMAGE: String = "mongo:7.0"
    }

    @Bean
    @ServiceConnection
    fun mongodb(): MongoDBContainer {
        return MongoDBContainer(DockerImageName.parse(MONGO_IMAGE))
    }
}

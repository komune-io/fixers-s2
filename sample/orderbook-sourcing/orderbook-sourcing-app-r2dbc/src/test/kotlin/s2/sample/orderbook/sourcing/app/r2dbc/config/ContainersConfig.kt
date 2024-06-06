package s2.sample.orderbook.sourcing.app.r2dbc.config

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer
import org.testcontainers.utility.MountableFile

@TestConfiguration(proxyBeanMethods = false)
class ContainersConfig {

    companion object {
        const val POSTGRES_IMAGE: String = "postgres:16-alpine"
    }

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(POSTGRES_IMAGE)
            .withUsername( "admin" )
            .withPassword( "admin" )
            .withDatabaseName("orderbook-sourcing")
    }
}

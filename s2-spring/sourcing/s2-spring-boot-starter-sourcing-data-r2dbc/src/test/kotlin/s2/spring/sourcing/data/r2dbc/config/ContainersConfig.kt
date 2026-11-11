package s2.spring.sourcing.data.r2dbc.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

@TestConfiguration(proxyBeanMethods = false)
open class ContainersConfig {

    @Bean
    @ServiceConnection
    open fun postgres(): PostgreSQLContainer {
        return PostgreSQLContainer(POSTGRES_IMAGE)
            .withUsername("admin")
            .withPassword("admin")
            .withDatabaseName("event-sourcing-test")
    }

    companion object {
        const val POSTGRES_IMAGE: String = "postgres:16-alpine"
    }
}

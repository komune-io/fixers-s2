package s2.spring.sourcing.data.r2dbc.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}

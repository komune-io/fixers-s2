package s2.sample.orderbook.sourcing.app.r2dbc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@EnableR2dbcRepositories(basePackages = ["s2.sample.orderbook.sourcing.app.r2dbc"])
@SpringBootApplication(scanBasePackages = ["s2"])
class SubAutomateR2dbcApp

fun main(args: Array<String>) {
	runApplication<SubAutomateR2dbcApp>(*args)
}

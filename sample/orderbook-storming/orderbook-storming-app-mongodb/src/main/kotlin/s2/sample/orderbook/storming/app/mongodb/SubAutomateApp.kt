package s2.sample.orderbook.storming.app.mongodb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EntityScan(basePackages=["s2.sample.orderbook.storming.app.mongodb"])
@EnableReactiveMongoRepositories(basePackages=["s2.sample.orderbook.storming.app.mongodb"])
@SpringBootApplication(scanBasePackages = ["s2.sample.orderbook.storming.app.mongodb"])
class SubAutomateApp

fun main(args: Array<String>) {
	runApplication<SubAutomateApp>(*args)
}

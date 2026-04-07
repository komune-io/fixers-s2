package s2.sample.orderbook.sourcing.app.mongodb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EntityScan(basePackages=["s2"])
@EnableReactiveMongoRepositories(basePackages=["s2"])
@SpringBootApplication(scanBasePackages = ["s2"])
class SubAutomateMongodbApp

fun main(args: Array<String>) {
	runApplication<SubAutomateMongodbApp>(*args)
}

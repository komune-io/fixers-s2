plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	`java-test-fixtures`
}

dependencies {
	api(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))
	api(libs.bundles.spring.redis)
	api(catalogue.spring.boot.starter.function.http)
	api(libs.jackson.module.kotlin)
	api(libs.kotlinx.serialization.json)

	testImplementation(libs.bundles.test.junit)
	testImplementation(libs.testcontainers.redis)
	testImplementation(libs.bundles.testcontainers)

	testFixturesApi(libs.spring.boot.starter.test)
	testFixturesApi(libs.bundles.testcontainers)
}

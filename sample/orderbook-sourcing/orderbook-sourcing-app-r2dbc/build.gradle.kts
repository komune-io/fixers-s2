plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.kotlin.serialization)
}

dependencies {
	api(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	api(project(":sample:orderbook-sourcing:orderbook-sourcing-core"))

	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data-r2dbc"))

	testImplementation(libs.bundles.testcontainers)
	testImplementation(libs.bundles.testcontainers.postgres)
	runtimeOnly(libs.postgresql)
	runtimeOnly(libs.r2dbc.postgresql)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}

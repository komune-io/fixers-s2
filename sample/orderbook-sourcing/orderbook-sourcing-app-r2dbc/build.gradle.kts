plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.spring.boot)
	alias(catalogue.plugins.kotlin.spring)
	alias(catalogue.plugins.kotlin.serialization)
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

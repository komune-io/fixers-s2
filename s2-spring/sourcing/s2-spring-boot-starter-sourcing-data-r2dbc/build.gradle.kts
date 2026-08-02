plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))
	api(libs.spring.boot.starter.data.r2dbc)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.kotlinx.coroutines.reactive)
	implementation(libs.kotlinx.coroutines.reactor)
	runtimeOnly(libs.postgresql)
	runtimeOnly(libs.r2dbc.postgresql)

	// Test dependencies
	testImplementation(libs.bundles.testcontainers)
	testImplementation(libs.bundles.testcontainers.postgres)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}

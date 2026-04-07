plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))
	api(libs.spring.boot.starter.data.r2dbc)
	implementation(libs.kotlinx.serialization.json)

	// Test dependencies
	testImplementation(libs.bundles.testcontainers)
	testImplementation(libs.bundles.testcontainers.postgres)
	runtimeOnly(libs.postgresql)
	runtimeOnly(libs.r2dbc.postgresql)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}

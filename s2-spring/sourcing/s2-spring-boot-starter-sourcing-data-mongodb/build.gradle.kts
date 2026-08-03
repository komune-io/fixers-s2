plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))
	api(libs.spring.boot.starter.data.mongodb.reactive)
	implementation(libs.bundles.kserialization.json)

	// Test dependencies
	// kotlinx-coroutines-reactor bridges the CoroutineCrudRepository suspend methods
	// (save/findAll/count) to their reactive equivalents at runtime.
	testImplementation(libs.kotlinx.coroutines.reactor)
	testImplementation(libs.bundles.testcontainers)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}

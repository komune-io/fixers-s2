plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.fixers.publish)
	alias(libs.plugins.kotlin.serialization)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))
	api(libs.spring.boot.starter.data.r2dbc)
	implementation(libs.bundles.kserialization.json)

	// Test dependencies
	testImplementation(libs.bundles.testcontainers)
	testImplementation(libs.bundles.testcontainers.postgres)
	testRuntimeOnly(libs.postgresql)
	testRuntimeOnly(libs.r2dbc.postgresql)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}

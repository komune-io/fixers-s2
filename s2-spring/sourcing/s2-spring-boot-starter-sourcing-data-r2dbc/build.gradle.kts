plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("plugin.serialization")
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))
	Dependencies.Spring.r2dbc(::api)
	Dependencies.kserializationJson(::implementation)

	// Test dependencies
	Dependencies.testcontainersPostgres(::testImplementation, ::runtimeOnly)
	Dependencies.springTest(::testImplementation)
}

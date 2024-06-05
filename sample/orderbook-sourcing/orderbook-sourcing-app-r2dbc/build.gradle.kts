plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("org.springframework.boot") version PluginVersions.springBoot
	kotlin("plugin.spring")
	kotlin("plugin.serialization")
}

dependencies {
	api(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))

	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data-r2dbc"))

	Dependencies.Fixers.f2Http (::implementation)
	Dependencies.kserializationJson (::implementation)
	Dependencies.Spring.redis(::api)

	Dependencies.testcontainersPostgres(::testImplementation, ::runtimeOnly)
	Dependencies.springTest(::testImplementation)
}

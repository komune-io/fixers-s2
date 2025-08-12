plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("org.springframework.boot") version PluginVersions.springBoot
	kotlin("plugin.spring")
	kotlin("plugin.serialization")
}

dependencies {
	api(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	api(project(":sample:orderbook-sourcing:orderbook-sourcing-core"))

	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data-mongodb"))

	Dependencies.testcontainers(::testImplementation)
	Dependencies.testcontainersRedis(::testImplementation)
	Dependencies.springTest(::testImplementation)
}

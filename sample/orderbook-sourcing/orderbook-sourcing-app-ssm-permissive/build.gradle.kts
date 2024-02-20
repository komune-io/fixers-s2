plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("org.springframework.boot") version PluginVersions.springBoot
	kotlin("plugin.spring")
	id("com.google.devtools.ksp") version PluginVersions.ksp
	kotlin("plugin.serialization")
}

dependencies {
	implementation(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	implementation(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-ssm"))

	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive:${Versions.springBoot}")

	implementation("io.lettuce:lettuce-core:6.1.6.RELEASE")

	Dependencies.Fixers.f2Http (::implementation)
	Dependencies.kserializationJson (::implementation)
	Dependencies.arrow (::implementation, ::ksp)

	Dependencies.springTest(::testImplementation)
}

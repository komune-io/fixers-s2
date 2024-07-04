plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("org.springframework.boot") version PluginVersions.springBoot
	kotlin("plugin.spring")
	id("com.google.devtools.ksp") version PluginVersions.ksp
	kotlin("plugin.serialization")
}

dependencies {
	implementation(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	implementation(project(":s2-spring:storing:s2-spring-boot-starter-storing-ssm"))

	Dependencies.Fixers.f2Http (::implementation)
	Dependencies.kserializationJson (::implementation)
	Dependencies.arrow (::implementation, ::ksp)

	Dependencies.testcontainers(::testImplementation)

	Dependencies.springTest(::testImplementation)
}

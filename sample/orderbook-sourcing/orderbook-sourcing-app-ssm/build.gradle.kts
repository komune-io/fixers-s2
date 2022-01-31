plugins {
	id("city.smartb.fixers.gradle.kotlin.jvm")
	id("org.springframework.boot") version PluginVersions.springBoot
	kotlin("plugin.spring")
	id("com.google.devtools.ksp") version PluginVersions.ksp
	kotlin("plugin.serialization")
}

dependencies {
	implementation(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	implementation(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-ssm"))
//	implementation("org.springframework.data:spring-data-redis:2.6.1")
//	implementation("redis.clients:jedis:4.1.0")

	Dependencies.f2Http (::implementation)
	Dependencies.kserializationJson (::implementation)
	Dependencies.arrow (::implementation, ::ksp)

	Dependencies.springTest(::testImplementation)
}

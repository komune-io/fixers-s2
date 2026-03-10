plugins {
	alias(libs.plugins.fixers.kotlin.jvm)

	alias(libs.plugins.kotlin.spring)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))

	implementation(libs.f2.spring.starter.function.http)
	api(libs.spring.boot.starter.data.mongodb.reactive)
}

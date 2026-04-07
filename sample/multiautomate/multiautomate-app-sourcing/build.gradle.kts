plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.kotlin.spring)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))

	implementation(catalogue.spring.boot.starter.function.http)
	api(libs.spring.boot.starter.data.mongodb.reactive)
}

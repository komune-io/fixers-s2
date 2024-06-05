plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-data"))
	Dependencies.Spring.r2dbc(::api)
}

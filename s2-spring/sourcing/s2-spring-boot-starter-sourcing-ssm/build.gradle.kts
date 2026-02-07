plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("kapt")
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))

	Dependencies.Spring.autoConfigure(::implementation, ::kapt)

	Dependencies.C2.ssmSourcing(::api)
}

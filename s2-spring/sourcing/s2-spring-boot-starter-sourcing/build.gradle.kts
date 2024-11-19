plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("kapt")
}

dependencies {
	api(project(":s2-spring:s2-spring-core"))

	Dependencies.Spring.tx(::implementation)
	Dependencies.Spring.autoConfigure(::implementation, ::kapt)
}

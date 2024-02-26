plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("kapt")
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))

	Dependencies.Spring.autoConfigure(::implementation, ::kapt)

	api("io.komune.ssm:ssm-data-spring-boot-starter:${Versions.ssm}")
	api("io.komune.ssm:ssm-tx-spring-boot-starter:${Versions.ssm}")
}

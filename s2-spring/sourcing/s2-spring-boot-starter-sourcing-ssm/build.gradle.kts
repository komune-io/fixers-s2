plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("kapt")
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))

	Dependencies.Spring.autoConfigure(::implementation, ::kapt)

	api("io.komune.c2:ssm-chaincode-spring-boot-starter:${Versions.c2}")
	api("io.komune.c2:ssm-data-spring-boot-starter:${Versions.c2}")

	api("io.komune.c2:ssm-tx-spring-boot-starter:${Versions.c2}")
}

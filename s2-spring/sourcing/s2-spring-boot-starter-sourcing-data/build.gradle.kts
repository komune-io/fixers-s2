plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("kapt")
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))
	api(project(":s2-spring:utils:s2-spring-boot-starter-utils-data"))

	Dependencies.Spring.autoConfigure(::implementation, ::kapt)
	Dependencies.Spring.dataCommons(::implementation)

}

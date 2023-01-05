plugins {
	id("city.smartb.fixers.gradle.kotlin.jvm")
	id("city.smartb.fixers.gradle.publish")
	kotlin("kapt")
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-spring:storing:s2-spring-boot-starter-storing"))
	api(project(":s2-spring:utils:s2-spring-boot-starter-utils-data"))

	Dependencies.Spring.dataCommons(::implementation)
}

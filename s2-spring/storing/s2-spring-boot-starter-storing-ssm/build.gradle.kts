plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("plugin.spring")
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-spring:storing:s2-spring-boot-starter-storing"))

	Dependencies.C2.ssmStoring(::api)
}

plugins {
	kotlin("plugin.spring")
	id("io.komune.fixers.gradle.kotlin.jvm")
}

dependencies {
	api(project(":sample:did-sample:did-domain"))
	api(project(":s2-spring:storing:s2-spring-boot-starter-storing-ssm"))

	Dependencies.springTest(::testImplementation)
}

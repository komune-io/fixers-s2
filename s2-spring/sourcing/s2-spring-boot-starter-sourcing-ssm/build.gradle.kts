plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.fixers.publish)
	alias(libs.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))

	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)

	api(libs.bundles.c2.ssm.sourcing)
}

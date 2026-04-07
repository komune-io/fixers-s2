plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))
	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)
	api(libs.bundles.c2.ssm.sourcing)
}

plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-spring:s2-spring-core"))
	implementation(libs.spring.tx)
	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)
}

plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.fixers.publish)
	alias(libs.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-spring:s2-spring-core"))

	implementation(libs.spring.tx)
	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)
}

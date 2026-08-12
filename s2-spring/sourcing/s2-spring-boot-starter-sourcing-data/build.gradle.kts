plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))
	api(project(":s2-spring:utils:s2-spring-boot-starter-utils-data"))
	api(libs.kotlinx.serialization.json)
	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)
	implementation(libs.bundles.spring.data.commons)
}

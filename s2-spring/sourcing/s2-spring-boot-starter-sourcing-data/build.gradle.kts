plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.fixers.publish)
	alias(libs.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing"))
	api(project(":s2-spring:utils:s2-spring-boot-starter-utils-data"))

	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)
	implementation(libs.bundles.spring.data.commons)
}

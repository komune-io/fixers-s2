plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-spring:storing:s2-spring-boot-starter-storing"))
	api(project(":s2-spring:utils:s2-spring-boot-starter-utils-data"))
	implementation(libs.bundles.spring.data.commons)
}

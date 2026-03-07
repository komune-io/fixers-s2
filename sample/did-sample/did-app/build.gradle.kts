plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.fixers.kotlin.jvm)
}

dependencies {
	api(project(":sample:did-sample:did-domain"))
	api(project(":s2-spring:storing:s2-spring-boot-starter-storing-ssm"))

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}

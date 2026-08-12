plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)

	// Only needed by applications that opt into S2SpringAdapterBase.validateRoles():
	// compileOnly keeps Spring Security off the classpath of everyone else.
	compileOnly(libs.spring.security.core)
	compileOnly(libs.kotlinx.coroutines.reactive)
	compileOnly(libs.kotlinx.coroutines.reactor)

	testImplementation(libs.bundles.test.junit)
	testImplementation(libs.spring.security.core)
	testImplementation(libs.kotlinx.coroutines.reactive)
	testImplementation(libs.kotlinx.coroutines.reactor)
}

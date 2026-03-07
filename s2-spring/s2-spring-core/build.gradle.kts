plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.fixers.publish)
	alias(libs.plugins.kotlin.kapt)
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	implementation(libs.spring.boot.autoconfigure)
	kapt(libs.spring.boot.configuration.processor)

	compileOnly(libs.c2.ssm.chaincode.dsl)
}

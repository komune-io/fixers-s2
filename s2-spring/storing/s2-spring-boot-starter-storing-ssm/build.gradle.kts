plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("plugin.spring")
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-spring:storing:s2-spring-boot-starter-storing"))

	api("io.komune.c2:ssm-chaincode-spring-boot-starter:${Versions.c2}")
	api("io.komune.c2:ssm-tx-spring-boot-starter:${Versions.c2}")
	api("io.komune.c2:ssm-tx-init-ssm-spring-boot-starter:${Versions.c2}")
}

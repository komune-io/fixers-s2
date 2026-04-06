plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.mpp)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	commonMainImplementation(project(":s2-automate:s2-automate-dsl"))
	commonMainApi(catalogue.client.core)
	commonMainApi(catalogue.client.ktor)

	jvmTestImplementation(project(":s2-automate:s2-automate-documenter"))
}

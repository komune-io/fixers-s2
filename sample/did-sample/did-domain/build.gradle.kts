plugins {
	alias(libs.plugins.fixers.kotlin.mpp)
	alias(libs.plugins.kotlin.serialization)
}

dependencies {
	commonMainImplementation(project(":s2-automate:s2-automate-dsl"))
	commonMainApi(libs.f2.client.ktor)

	jvmTestImplementation(project(":s2-automate:s2-automate-documenter"))
}

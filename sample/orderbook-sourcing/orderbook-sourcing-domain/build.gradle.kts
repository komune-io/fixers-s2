plugins {
	alias(libs.plugins.fixers.kotlin.mpp)
	alias(libs.plugins.ksp)
	alias(libs.plugins.kotlin.serialization)
}

dependencies {
	commonMainApi(project(":s2-automate:s2-automate-dsl"))
	commonMainApi(libs.arrow.core)
	commonMainApi(libs.arrow.optics)
	add("kspCommonMainMetadata", libs.arrow.optics.ksp)
}

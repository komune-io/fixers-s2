plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.mpp)
	alias(catalogue.plugins.fixers.gradle.publish)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	commonMainApi(libs.f2.dsl.cqrs)
	commonMainApi(libs.kotlinx.serialization.core)

	"jvmTestImplementation"(libs.bundles.test.junit)
}

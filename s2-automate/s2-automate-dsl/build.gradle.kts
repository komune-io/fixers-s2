plugins {
    alias(catalogue.plugins.fixers.gradle.kotlin.mpp)
    alias(catalogue.plugins.fixers.gradle.publish)
    alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
    commonMainApi(libs.f2.dsl.cqrs)
    commonMainApi(libs.kotlinx.serialization.core)

    commonTestImplementation(kotlin("test"))
    "jvmTestImplementation"(kotlin("test-junit5"))
    "jvmTestImplementation"(libs.bundles.test.junit)
    "jsTestImplementation"(kotlin("test-js"))
}

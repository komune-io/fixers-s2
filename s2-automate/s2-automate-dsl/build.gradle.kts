
plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	id("io.komune.fixers.gradle.publish")
	kotlin("plugin.serialization")
//	id("dev.petuska.npm.publish")
}

dependencies {
    commonMainApi("io.komune.c2:ssm-chaincode-dsl:${Versions.ssm}")
}

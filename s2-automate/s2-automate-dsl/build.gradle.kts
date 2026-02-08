
plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	id("io.komune.fixers.gradle.publish")
	kotlin("plugin.serialization")
}

dependencies {
    Dependencies.C2.ssmChaincodeDsl(::commonMainApi)
}

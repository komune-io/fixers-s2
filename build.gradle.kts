plugins {
	alias(libs.plugins.kotlin.spring) apply false
	alias(libs.plugins.kotlin.serialization) apply false
	alias(libs.plugins.kotlin.kapt) apply false
	alias(libs.plugins.node.gradle)
	alias(libs.plugins.fixers.config)
	alias(libs.plugins.fixers.check)
	alias(libs.plugins.fixers.publish)
	alias(libs.plugins.fixers.d2)
	alias(libs.plugins.fixers.kotlin.mpp) apply false
	alias(libs.plugins.fixers.kotlin.jvm) apply false
}

allprojects {
	group = "io.komune.s2"
	version = System.getenv("VERSION") ?: "experimental-SNAPSHOT"
	repositories {
		if (System.getenv("MAVEN_LOCAL_USE") == "true") {
			mavenLocal()
		}
		mavenCentral()
		maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
	}
}

subprojects {
	pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
		dependencies {
			"api"(platform(libs.f2.bom))
		}
	}
	pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
		dependencies {
			"commonMainApi"(platform(libs.f2.bom))
		}
	}
}


tasks {
	register<com.github.gradle.node.yarn.task.YarnTask>("installYarn") {
		dependsOn("build")
		args = listOf("install")
	}

	register<com.github.gradle.node.yarn.task.YarnTask>("storybook") {
		dependsOn("yarn_install")
		args = listOf("storybook")
	}
}

fixers {
	d2 {
		outputDirectory = file("storybook/d2/")
	}
	bundle {
		id = "s2"
		name = "S2"
		description = "Fixers S2"
		url = "https://github.com/komune-io/fixers-s2"
	}
	npm {
		organization = "komune"
	}
	sonar {
		organization = "komune-io"
		projectKey = "komune-io_fixers-s2"
	}
}

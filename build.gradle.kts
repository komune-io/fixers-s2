plugins {
	alias(catalogue.plugins.kotlin.spring) apply false
	alias(catalogue.plugins.kotlin.serialization) apply false
	alias(catalogue.plugins.kotlin.kapt) apply false
	alias(libs.plugins.node.gradle)

	alias(catalogue.plugins.f2.bom)
	alias(catalogue.plugins.fixers.gradle.config)
	alias(catalogue.plugins.fixers.gradle.check)

	alias(catalogue.plugins.fixers.gradle.kotlin.mpp) apply false
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm) apply false
	alias(catalogue.plugins.fixers.gradle.publish) apply false
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
	bundle {
		id = "s2"
		group = "io.komune.s2"
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
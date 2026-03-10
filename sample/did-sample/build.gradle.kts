plugins {
	alias(libs.plugins.spring.boot) apply false
}

tasks {
	register("cleanKts", Delete::class) {
		delete("did-ui/kotlin")
	}

	register("kts", Copy::class) {
		dependsOn("cleanKts")
		from("${layout.buildDirectory.get().asFile.absolutePath}/js/packages/") {
			exclude("*-test")
		}

		into("did-ui/kotlin")
		includeEmptyDirs = false
	}
}

plugins {
}

//tasks {
//	create<com.moowork.gradle.node.yarn.YarnTask>("installYarn") {
//		dependsOn("build")
//		args = listOf("install")
//	}
//
//	create<com.moowork.gradle.node.yarn.YarnTask>("storybook") {
////        dependsOn("yarn_nstall")
//		args = listOf("storybook")
//	}
//
//	register("clean", Delete::class) {
//		delete("kotlin/*")
//	}
//
//	register("build", Copy::class) {
//		from("${this.project.rootProject.buildDir.absolutePath}/js/packages/") {
//			exclude("*-test")
//			exclude("**/terser-webpack-plugin")
//		}
//
//		into("kotlin")
//		includeEmptyDirs = false
//	}
//}

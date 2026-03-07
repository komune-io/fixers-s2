plugins {
	alias(libs.plugins.fixers.kotlin.mpp)
	alias(libs.plugins.fixers.publish)
	alias(libs.plugins.kotlin.serialization)
}

dependencies {
	commonMainApi(libs.c2.ssm.chaincode.dsl)
}

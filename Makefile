STORYBOOK_DOCKERFILE	:= infra/docker/storybook/Dockerfile
STORYBOOK_NAME	   	 	:= komune/s2-storybook
STORYBOOK_IMG	    	:= ${STORYBOOK_NAME}:${VERSION}
STORYBOOK_LATEST		:= ${STORYBOOK_NAME}:latest

VERSION = $(shell cat VERSION)

lint: lint-libs
build: build-libs
test: test-libs
publish: publish-libs
promote: promote-libs

libs: package-kotlin

package-kotlin: build-libs publish-libs

lint-libs:
	echo 'No Lint'
	#./gradlew detekt

build-libs:
	./gradlew build -x test publishToMavenLocal

test-libs:
	echo 'No Tests'
#	./gradlew test

publish-libs: build-libs
	PKG_MAVEN_REPO=github ./gradlew publish

promote-libs: build-libs
	PKG_MAVEN_REPO=sonatype_oss ./gradlew publish

.PHONY: version
version:
	echo "$$VERSION"

## DEV ENVIRONMENT
include infra/docker-compose/dev-compose.mk

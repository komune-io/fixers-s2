STORYBOOK_DOCKERFILE	:= infra/docker/storybook/Dockerfile
STORYBOOK_NAME	   	 	:= komune/s2-storybook
STORYBOOK_IMG	    	:= ${STORYBOOK_NAME}:${VERSION}
STORYBOOK_LATEST		:= ${STORYBOOK_NAME}:latest

lint: lint-libs
build: build-libs
test: test-libs
package: package-libs

libs: package-kotlin

package-kotlin: build-libs publish-libs

lint-libs:
	echo 'No Lint'
	#./gradlew detekt

build-libs:
	./gradlew build --scan -x test

test-libs:
	echo 'No Tests'
#	./gradlew test

package-libs: build-libs
	./gradlew publishToMavenLocal publish

version:
	@VERSION=$$(cat VERSION); \
	echo "$$VERSION"

## DEV ENVIRONMENT
include infra/docker-compose/dev-compose.mk

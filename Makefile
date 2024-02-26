STORYBOOK_DOCKERFILE	:= infra/docker/storybook/Dockerfile
STORYBOOK_NAME	   	 	:= komune/s2-storybook
STORYBOOK_IMG	    	:= ${STORYBOOK_NAME}:${VERSION}
STORYBOOK_LATEST		:= ${STORYBOOK_NAME}:latest

lint: lint-libs
build: build-libs
test: test-libs
package: package-libs

libs: package-kotlin
docs: package-storybook

package-kotlin: build-libs publish-libs

package-storybook:
	@docker build --no-cache --build-arg CI_NPM_AUTH_TOKEN=${CI_NPM_AUTH_TOKEN} -f ${STORYBOOK_DOCKERFILE} -t ${STORYBOOK_IMG} .
	@docker push ${STORYBOOK_IMG}

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

package-storybook:
	@docker build --build-arg CI_NPM_AUTH_TOKEN=${CI_NPM_AUTH_TOKEN} -f ${STORYBOOK_DOCKERFILE} -t ${STORYBOOK_IMG} .

push-storybook:
	@docker push ${STORYBOOK_IMG}

version:
	@VERSION=$$(cat VERSION); \
	echo "$$VERSION"

## DEV ENVIRONMENT
include infra/docker-compose/dev-compose.mk

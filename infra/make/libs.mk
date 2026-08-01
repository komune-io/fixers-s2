VERSION = $(shell cat VERSION)

.PHONY: clean lint build test check publish promote version

clean:
	./gradlew clean

lint:
	./gradlew detekt

build:
	VERSION=$(VERSION) ./gradlew clean build publishToMavenLocal -x test

test:
	./gradlew test

check:
	VERSION=$(VERSION) ./gradlew sonar

stage:
	VERSION=$(VERSION) ./gradlew stage

promote:
	VERSION=$(VERSION) ./gradlew promote

.PHONY: version
version:
	@echo "$(VERSION)"

VERSION = $(shell cat VERSION)

.PHONY: clean lint build test check publish promote version verify-metadata verify-metadata-dry

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

verify-metadata: verify-metadata-dry
	mv gradle/verification-metadata.dryrun.xml gradle/verification-metadata.xml
	mv gradle/verification-keyring.dryrun.keys gradle/verification-keyring.keys
	mv gradle/verification-keyring.dryrun.gpg gradle/verification-keyring.gpg

# Generates the same files with the .dryrun suffix, to inspect the delta without replacing anything.
verify-metadata-dry:
	./gradlew --write-verification-metadata pgp,sha256 --export-keys --dry-run build publishToMavenLocal

.PHONY: version
version:
	@echo "$(VERSION)"

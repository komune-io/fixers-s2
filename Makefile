VERSION = $(shell cat VERSION)

.PHONY: clean lint build test check publish promote

clean:
	@make -f infra/make/libs.mk clean

lint:
	@make -f infra/make/libs.mk lint

build:
	@make -f infra/make/libs.mk build

test:
	@make -f infra/make/libs.mk test

check:
	@make -f infra/make/libs.mk check

stage:
	@make -f infra/make/libs.mk stage

promote:
	@make -f infra/make/libs.mk promote

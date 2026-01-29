VERSION = $(shell cat VERSION)

.PHONY: lint build test publish promote

## New
lint:
	@make -f infra/script/make_libs.mk lint
	@make -f infra/script/make_docs.mk lint

build:
	@make -f infra/script/make_libs.mk build
	@make -f infra/script/make_docs.mk build

test-pre:
	@make -f infra/script/make_libs.mk test-pre

test:
	@make -f infra/script/make_libs.mk test
	@make -f infra/script/make_docs.mk test

stage:
	@make -f infra/script/make_libs.mk stage
	@make -f infra/script/make_docs.mk stage

promote:
	@make -f infra/script/make_libs.mk promote
	@make -f infra/script/make_docs.mk promote

## DEV ENVIRONMENT
include infra/docker-compose/dev-compose.mk

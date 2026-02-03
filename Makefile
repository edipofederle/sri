.PHONY: all build test clean repl run native help

# Default target
all: test

# Build the uberjar
build:
	lein uberjar

# Build native binary
native: build
	./build-native.sh

# Run all tests (builds native first)
test: native
	lein test

# Run only JVM tests (faster, no native build)
test-jvm:
	lein test sri.attr-test sri.core-test sri.parser-test sri.ruby-classes-test sri.tokenizer-test

# Run only native integration tests
test-native: native
	lein test sri.native-integration-test

# Clean build artifacts
clean:
	lein clean
	rm -f target/sri

# Start a REPL
repl:
	lein repl

# Run a Ruby file
run:
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make run FILE=path/to/file.rb"; \
	else \
		lein run $(FILE); \
	fi

# Run a Ruby file with native binary
run-native: native
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make run-native FILE=path/to/file.rb"; \
	else \
		./target/sri $(FILE); \
	fi

# Run RSpec test file
rspec:
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make rspec FILE=path/to/spec.rb"; \
	else \
		lein run test_rspec_capabilities.rb $(FILE); \
	fi

# Run Ruby specs from ruby-specs-to-run.edn
ruby-specs:
	@total_pass=0; total_fail=0; total_error=0; \
	for spec in $$(clj -M -e '(doseq [s (-> "ruby-specs-to-run.edn" slurp clojure.edn/read-string)] (println (:path s)))' 2>/dev/null); do \
		echo ""; \
		echo "=== Running: $$spec ==="; \
		output=$$(lein run test_rspec_capabilities.rb "$$spec" 2>&1); \
		echo "$$output"; \
		pass=$$(echo "$$output" | grep -oE '^[0-9]+ pass' | grep -oE '[0-9]+' || echo 0); \
		fail=$$(echo "$$output" | grep -oE '[0-9]+ fail' | grep -oE '[0-9]+' || echo 0); \
		error=$$(echo "$$output" | grep -oE '[0-9]+ error' | grep -oE '[0-9]+' || echo 0); \
		total_pass=$$((total_pass + pass)); \
		total_fail=$$((total_fail + fail)); \
		total_error=$$((total_error + error)); \
	done; \
	echo ""; \
	echo "========================================"; \
	echo "TOTAL: $$total_pass pass, $$total_fail fail, $$total_error error"; \
	echo "========================================"

# Quick development cycle - just run JVM tests
dev: test-jvm

# Full CI build and test
ci: clean native test

# Show help
help:
	@echo "Sri Ruby Interpreter - Makefile targets:"
	@echo ""
	@echo "  make              - Run all tests (default)"
	@echo "  make build        - Build uberjar"
	@echo "  make native       - Build native binary"
	@echo "  make test         - Build native and run all tests"
	@echo "  make test-jvm     - Run JVM tests only (faster)"
	@echo "  make test-native  - Run native integration tests"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make repl         - Start a Clojure REPL"
	@echo "  make dev          - Quick dev cycle (JVM tests only)"
	@echo "  make ci           - Full CI build (clean + native + test)"
	@echo ""
	@echo "  make run FILE=x.rb       - Run Ruby file with JVM"
	@echo "  make run-native FILE=x.rb - Run Ruby file with native binary"
	@echo "  make rspec FILE=spec.rb  - Run RSpec-style test file"
	@echo "  make ruby-specs          - Run specs from ruby-specs-to-run.edn"

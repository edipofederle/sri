# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sri is a Ruby interpreter implemented in Clojure, targeting safe Ruby evaluation (similar to SCI for Clojure). It uses Leiningen as its build tool and supports GraalVM native image compilation.

## Common Commands

```bash
# Run JVM tests (fast dev cycle, no native build required)
make dev                    # or: make test-jvm

# Run a single test namespace
lein test sri.core-test

# Run a single test
lein test :only sri.core-test/test-basic-arithmetic

# Run a Ruby file
lein run examples/arithmetic.rb

# Evaluate a Ruby expression
lein run -e "10 + 20"

# Run a single Ruby spec
make rspec FILE=spec/language/numbers_spec.rb

# Run all configured Ruby specs (listed in ruby-specs-to-run.edn)
make ruby-specs

# Build uberjar
lein uberjar

# Full test suite (builds native binary first, slow)
make test

# REPL
lein repl
```

## Architecture

### Interpreter Pipeline

```
Ruby Source → Tokenizer → Tokens → Parser → AST → Interpreter → Result
```

- **`src/sri/tokenizer.cljc`** — Lexical analyzer. Converts Ruby source to tokens.
- **`src/sri/parser.cljc`** — Parser using an ECS-based flat AST (entities are integer IDs, components are node-type/value/children). See `doc/parsing.md`.
- **`src/sri/interpreter.cljc`** — Direct AST interpreter. Evaluates expressions/statements, handles method lookup and scoping.
- **`src/sri/core.clj`** — Main entry point. `eval-string` is the public API.

### Ruby Object System

Protocol-based hierarchy mirroring Ruby's class structure:

- **`src/sri/ruby_protocols.cljc`** — Core protocols: `RubyObject`, `RubyInspectable`, `RubyComparable`
- **`src/sri/ruby_method_registry.cljc`** — Centralized method lookup registry
- **`src/sri/ruby_kernel.cljc`** — Kernel module (puts, print, p, etc.)
- **`src/sri/enumerable.cljc`** — Enumerable module shared by Array and Range
- **`src/sri/ruby_*.cljc`** — Individual class implementations (String, Array, Hash, Range, Symbol, Numeric, etc.)

Class hierarchy: `BasicObject → Object (includes Kernel) → String/Integer/Array/Hash/etc.`

### Key Design Details

- **String interpolation**: `#{}` expressions are stored as raw source text in tokens, then re-tokenized and re-parsed at interpretation time.
- **Method definitions**: Store a reference to the original AST (important because string interpolation creates temporary ASTs).
- **All source files use `.cljc`** extension for cross-compilation compatibility.
- **`src/java/sri/Sri.java`** — Java API wrapper for embedding.

## Testing

- **Clojure tests** in `test/sri/` — Unit tests for tokenizer, parser, interpreter, and Ruby classes.
- **Ruby spec suite** in `spec/` (git submodule) — Forked Ruby Spec Suite for compatibility testing. Controlled by `ruby-specs-to-run.edn`.
- **`test_rspec_capabilities.rb`** — Custom RSpec-like test runner that Sri executes to run Ruby specs.
- Native integration tests (`sri.native-integration-test`) require building the native binary first.

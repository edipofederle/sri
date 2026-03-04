# Sri - Ruby Interpreter

A Ruby interpreter implemented in Clojure with support for GraalVM native image compilation. (VERY early stage. but already can run basic ruby program (see example directory))
## Building

### Prerequisites
- Leiningen
- Java 11+
- GraalVM (optional but recommended, for native image compilation)

### Build JAR
```bash
lein uberjar
```

### Build Native Image (requires GraalVM)
```bash
./build-native.sh
```

## Usage

### As a Library

Sri can now be used as a Clojure library for safe Ruby evaluation:

```clojure
(require '[sri.core :as sri])

;; Basic evaluation
(sri/eval-string "1 + 2")                    ; => 3
(sri/eval-string "puts('Hello World!')")     ; prints "Hello World!", returns nil

;; With custom variables
(sri/eval-string "name + ' is ' + age.to_s + ' years old'"
                 {:namespaces {"name" "Alice" "age" 30}})
; => "Alice is 30 years old"

;; Method definitions and calls
(sri/eval-string "def greet(name); 'Hello ' + name; end; greet('Bob')")
; => "Hello Bob"

;; Array operations
(sri/eval-string "[1, 2, 3, 4, 5].length")   ; => 5
```

### Java API

Sri provides a Java wrapper for easy integration with Java applications:

```java
import sri.Sri;

public class Example {
    public static void main(String[] args) {
        // Basic evaluation
        System.out.println(Sri.eval("10 + 20"));           // 30
        System.out.println(Sri.eval("'hello'.upcase"));    // HELLO

        // Array operations
        System.out.println(Sri.eval("[1, 2, 3].map { |x| x * 2 }"));  // [2, 4, 6]

        // Method definitions
        System.out.println(Sri.eval("def greet(name); 'Hello ' + name; end; greet('World')"));
        // Hello World
    }
}
```

Compile and run:

```bash
javac -cp target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar Example.java
java -cp .:target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar Example
```

### Command Line Interface

#### Evaluate Expressions

Evaluate Ruby expressions directly from the command line using the `-e` flag:

```bash
# With Leiningen
lein run -e "10 + 20"                           # => 30
lein run -e "'hello' + ' ' + 'world'"          # => hello world
lein run -e "[1, 2, 3].length"                 # => 3

# With Java
java -jar target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar -e "10 + 20"

# With Native Binary (if built)
./target/sri -e "10 + 20"
```

#### Execute Ruby Files

Run Ruby programs from files:

```bash
# With Leiningen
lein run <ruby-file>

# With Java
java -jar target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar <ruby-file>

# With Native Binary (if built)
./target/sri <ruby-file>
```

## Examples

The `examples/` directory contains various Ruby programs to test the interpreter:

```bash
# Run a simple method example
lein run examples/working_methods.rb

# Run arithmetic operations
lein run examples/arithmetic.rb

# Run class definitions
lein run examples/class_basic.rb
```

## Testing

Run the test suite:
```bash
lein test
```

### Running Ruby Specs

This is in a **very early stage**, but it's possible to run some specs from the [Ruby Spec Suite](https://github.com/edipofederle/spec) (forked version for now) against Sri:

```bash
make ruby-specs
```

This will run the specs listed at `ruby-specs-to-run.edn` for now.

Note: Only a VERY small subset of specs currently pass (for example `numbers_spec.rb`, `array_spec.rb`)  This is primarily useful for development and tracking progress toward Ruby compatibility.

## Development

### Project Structure

#### Interpreter pipeline
- `src/sri/core.clj` — Main entry point; `eval-string` public API
- `src/sri/tokenizer.cljc` — Lexical analyzer
- `src/sri/parser.cljc` — ECS-based flat AST parser
- `src/sri/interpreter.cljc` — Direct AST interpreter

#### Ruby object system
- `src/sri/ruby_protocols.cljc` — Core protocols: `RubyObject`, `RubyInspectable`, `RubyComparable`
- `src/sri/ruby_method_registry.cljc` — Centralized method lookup registry
- `src/sri/ruby_basic_object.cljc` — `BasicObject` base
- `src/sri/ruby_object.cljc` — `Object` (includes Kernel)
- `src/sri/ruby_kernel.cljc` — Kernel module (`puts`, `print`, `p`, etc.)
- `src/sri/ruby_classes_new.cljc` — Class/instance creation helpers
- `src/sri/enumerable.cljc` — Enumerable module shared by Array and Range
- `src/sri/ruby_string.cljc` — String class
- `src/sri/ruby_array.cljc` — Array class
- `src/sri/ruby_hash.cljc` — Hash class
- `src/sri/ruby_numeric.cljc` — Numeric / Integer / Float
- `src/sri/ruby_range.cljc` — Range class
- `src/sri/ruby_symbol.cljc` — Symbol class
- `src/sri/ruby_nil_class.cljc` — NilClass
- `src/sri/ruby_true_class.cljc` — TrueClass
- `src/sri/ruby_false_class.cljc` — FalseClass
- `src/sri/ruby_exception.cljc` — Exception hierarchy
- `src/sri/ruby_rational.cljc` — Rational class
- `src/sri/ruby_complex.cljc` — Complex class

#### Java integration
- `src/java/sri/Sri.java` — Java API wrapper for embedding

#### Tests & specs
- `test/sri/` — Clojure unit tests
- `spec/` — Forked Ruby Spec Suite (git submodule)
- `ruby-specs-to-run.edn` — Specs enabled for CI
- `test_rspec_capabilities.rb` — Custom RSpec-like test runner

#### Documentation & examples
- `doc/` — Design docs and notes
- `examples/` — Ruby programs demonstrating interpreter features

## License

Copyright © 2025 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

# Sri - Ruby Interpreter

A Ruby interpreter implemented in Clojure with support for GraalVM native image compilation.
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

### Command Line Interface

### Running with Java
```bash
java -jar target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar <ruby-file>
```

### Running with Native Binary (if built)
```bash
./target/sri <ruby-file>
```

### Running with Leiningen
```bash
lein run <ruby-file>
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

## Development

### Project Structure
- `src/sri/core.clj` - Main entry point
- `src/sri/tokenizer.cljc` - Lexical analyzer
- `src/sri/parser.cljc` - AST parser
- `src/sri/interpreter.cljc` - AST interpreter
- `examples/` - Ruby test programs

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

# SRI Ruby Interpreter - Supported Features

**Status Legend:**
- ✅ **Fully Working** - Feature tested and works in examples
- 🔧 **Implemented but Untested** - Code exists but no working examples
- ❌ **Not Implemented** - Feature not available

## Core Language Features

### Numbers and Arithmetic
| Feature | Example | Status |
|---------|---------|--------|
| Integer literals | `42`, `1234`, `4_3_5_7` | ✅ |
| Float literals | `3.14`, `0.75` | ✅ |
| Negative numbers | `-5`, `-3.14` | ✅ |
| Arithmetic operations | `+`, `-`, `*`, `/` | ✅ |
| Order of operations | `10 - 5 * 3` → `-5` | ✅ |
| Integer methods | `.positive?`, `.negative?`, `.zero?`, `.even?` | ✅ |
| Integer utility methods | `.inc`, `.incn(5)`, `.double` | ✅ |
| Integer class methods | `Integer.max(100,20)`, `Integer.sqrt(625)` | ✅ |
| Type checking | `.real?`, `.integer?` | ✅ |
| Hexadecimal literals | `0xffff`, `0XFFFF` | 🔧 |
| Binary literals | `0b01011`, `0B01011` | 🔧 |
| Octal literals | `0377` | 🔧 |
| Scientific notation | `1.2e-3` | 🔧 |
| Rational literals | `3r`, `1.0r`, `0xffr` | 🔧 |
| Complex literals | `5i`, `0.6i`, `0xffi` | 🔧 |

### Strings
| Feature | Example | Status |
|---------|---------|--------|
| String literals | `"hello"`, `'world'` | ✅ |
| String concatenation | `"Hello " + "World"` | ✅ |
| String interpolation | `"#{name} is #{age} years old"` | ✅ |
| Expression interpolation | `"Sum: #{10 + 20}"` | ✅ |
| String conversion | `.to_s` on numbers | ✅ |
| String indexing | `str[0]`, `str[-1]` | ✅ |
| String slicing | `str[1, 3]` | ✅ |
| String methods | `.length`, `.size`, `.empty?` | ✅ |
| String comparison | `==`, `!=` | ✅ |

### Symbols
| Feature | Example | Status |
|---------|---------|--------|
| Symbol literals | `:hello`, `:ruby_symbol` | ✅ |
| Symbol methods | `.to_s`, `.inspect` | ✅ |
| Symbol properties | `.length`, `.size` | ✅ |
| Symbol conversion | `.id2name` | ✅ |

### Arrays
| Feature | Example | Status |
|---------|---------|--------|
| Array literals | `[1, 2, 3]`, `[]` | ✅ |
| Array access | `arr[0]`, `arr[-1]` | ✅ |
| Array assignment | `arr[0] = 42` | ✅ |
| Array expansion | `arr[7] = 99` (auto-fills with nil) | ✅ |
| Array methods | `.length`, `.first`, `.last`, `.empty?` | ✅ |
| Array iteration | `.each do |x|` and `.each { |x| }` | ✅ |
| Mixed type arrays | `[1, "hello", :symbol]` | ✅ |

### Hashes
| Feature | Example | Status |
|---------|---------|--------|
| Hash literals | `{"key" => "value"}`, `{}` | ✅ |
| Hash access | `hash["key"]` | ✅ |
| Hash assignment | `hash["key"] = "value"` | ✅ |
| Mixed keys/values | `{1 => "A", "name" => "Alice"}` | ✅ |
| Hash methods | `.size`, `.length`, `.empty?` | ✅ |
| Key checking | `.key?`, `.include?`, `.member?` | ✅ |
| Key/value access | `.keys`, `.values` | ✅ |
| Hash modification | `.delete`, `.remove` | ✅ |

### Ranges
| Feature | Example | Status |
|---------|---------|--------|
| Inclusive ranges | `1..5` | ✅ |
| Exclusive ranges | `1...5` | ✅ |
| Range methods | `.to_a`, `.include?`, `.size`, `.count` | ✅ |
| Negative ranges | `-2..2` | ✅ |
| Single element ranges | `5..5` | ✅ |
| Empty ranges | `5...5` | ✅ |
| Range in case statements | `when 1..3` | ✅ |

## Object-Oriented Programming

### Classes
| Feature | Example | Status |
|---------|---------|--------|
| Class definition | `class Person; end` | ✅ |
| Class instantiation | `Person.new` | ✅ |
| Constructor methods | `def initialize(name); end` | ✅ |
| Instance variables | `@name = name` | ✅ |
| Instance methods | `def greet; end` | ✅ |
| Method parameters | `def initialize(name, age)` | ✅ |
| Attr accessors | `attr_accessor :name, :age` | ✅ |
| Attr readers | `attr_reader :name` | ✅ |
| Attr writers | `attr_writer :age` | ✅ |

### Methods
| Feature | Example | Status |
|---------|---------|--------|
| Method definition | `def method_name; end` | ✅ |
| Method parameters | `def max(a, b); end` | ✅ |
| Method calls | `object.method_name` | ✅ |
| Method return values | `return value` or implicit return | ✅ |
| Method chaining | `object.method1.method2` | ✅ |

## Control Flow

### Conditionals
| Feature | Example | Status |
|---------|---------|--------|
| If statements | `if condition; end` | ✅ |
| If-else statements | `if condition; else; end` | ✅ |
| Inline conditionals | `if a > b; a; else; b; end` | ✅ |
| Case-when statements | `case var; when 1..3; end` | ✅ |
| Comparison operators | `>`, `<`, `>=`, `<=`, `==`, `!=` | ✅ |

### Blocks and Iteration
| Feature | Example | Status |
|---------|---------|--------|
| Block syntax (do-end) | `.each do |item|; end` | ✅ |
| Block syntax (braces) | `.each { |item| }` | ✅ |
| Block parameters | `|x|`, `|key, value|` | ✅ |
| Array iteration | `[1,2,3].each { |x| puts x }` | ✅ |
| Yield statements | `yield value` | ✅ |

### Loops
| Feature | Example | Status |
|---------|---------|--------|
| For loops | `for i in 1..5; end` | ✅ |
| While loops | `while condition; end` | ✅ |
| Until loops | `until condition; end` | ✅ |

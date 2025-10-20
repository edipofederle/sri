# Sri Ruby Interpreter - Feature TODO

This document tracks Ruby features to be implemented in the Sri interpreter, organized by priority and complexity.

## High Priority - Core Ruby Features

### String Interpolation
- [x] `"Hello #{variable}!"` syntax
- [x] `"Result: #{expression + 1}"` with expressions
- [ ] Nested interpolation support (future enhancement)
- [ ] Escape sequences in interpolated strings (future enhancement)
- **Impact**: High - Essential Ruby feature
- **Complexity**: Medium - Requires tokenizer and parser changes
- **Status**: Basic string interpolation implemented and working

### Loops & Iteration
- [x] `for item in array` loops
- [x] `until condition` loops (inverse of while)
- [x] `break` and `next` in all loop types
- [x] `loop do...end` infinite loops
- **Impact**: High - Fundamental control flow
- **Complexity**: Low-Medium - Extends existing loop infrastructure
- **Status**: All loop types (for, until, while, loop do...end) with break/next implemented and working

### Ranges ✅ COMPLETED
- [x] `1..10` (inclusive) and `1...10` (exclusive) literals ✅ COMPLETED
- [x] Range methods: `.each`, `.to_a`, `.include?` ✅ COMPLETED
- [x] Range in case statements (when 1..5) ✅ COMPLETED
- [x] Character ranges `"a".."z"` ✅ COMPLETED
- **Impact**: High - Used everywhere in Ruby
- **Complexity**: Medium - New literal type + methods
- **Status**: ✅ Integer and character ranges with core methods (.to_a, .include?, .size, .count, .to_s) and case statement support implemented and working

### Enumerable Module ✅ COMPLETED
- [x] Enumerable architecture (separate module) ✅ COMPLETED
- [x] Core methods: `.each`, `.map`/`.collect`, `.select`/`.filter` ✅ COMPLETED
- [x] Additional methods: `.reject`, `.find`/`.detect`, `.any?`, `.all?` ✅ COMPLETED
- [x] Works with both Arrays and Ranges ✅ COMPLETED
- [x] Eliminates code duplication ✅ COMPLETED
- **Impact**: High - Provides Ruby-like enumerable functionality
- **Complexity**: Medium - Modular architecture with duck typing
- **Status**: ✅ Complete enumerable module with Ruby-compatible methods for all collection types

### Exception Handling
- [ ] `begin/rescue/ensure/end` blocks
- [ ] `raise` statement for throwing exceptions
- [ ] Exception classes and hierarchy
- [ ] `retry` in rescue blocks
- **Impact**: High - Critical for robust code
- **Complexity**: High - Requires exception flow control

## Medium Priority - Enhanced Features

### Blocks & Iterators Enhancement
- [ ] `yield` keyword for method-defined blocks
- [ ] Block return values and early returns
- [ ] `5.times {|i| ... }` numeric iteration
- [ ] `1.upto(10) {|i| ... }` and `10.downto(1) {|i| ... }`
- [ ] `array.each_with_index {|item, index| ... }`
- **Impact**: Medium-High - Makes Ruby more expressive
- **Complexity**: Medium - Extends existing block system

### Regular Expressions
- [ ] `/pattern/` and `%r{pattern}` literals
- [ ] Match operator `=~` and `!~`
- [ ] String methods: `.match`, `.scan`, `.gsub`, `.sub`
- [ ] Global variables `$1`, `$2` for captures
- [ ] Regular expression options (i, m, x)
- **Impact**: Medium - Important for text processing
- **Complexity**: High - Requires regex engine integration

### Constants & Symbols  PARTIALLY COMPLETED
- [ ] `CONSTANT = value` constant definitions
- [ ] Constant scoping and lookup
- [x] `:symbol` literal syntax  COMPLETED
- [x] Symbol methods and symbol table  COMPLETED
- [ ] Constant warnings on reassignment
- **Impact**: Medium - Core Ruby concepts
- **Complexity**: Medium - Symbol table management
- **Status**:  Symbol literals and methods (.to_s, .inspect, .length) implemented and working

### Modules & Mixins
- [ ] `module ModuleName` definitions
- [ ] `include ModuleName` in classes
- [ ] `extend ModuleName` for singleton methods
- [ ] Module constants and methods
- [ ] Namespacing with `::`
- **Impact**: Medium-High - Essential for larger programs
- **Complexity**: High - Complex scoping and method resolution

## Lower Priority - Advanced Features

### Metaprogramming
- [ ] `define_method` for dynamic method creation
- [ ] `method_missing` for dynamic method handling
- [ ] `eval` and `instance_eval`
- [ ] `const_get` and `const_set`
- [ ] Method introspection (`.methods`, `.respond_to?`)
- **Impact**: Low-Medium - Advanced Ruby features
- **Complexity**: Very High - Runtime code generation

### Advanced OOP
- [ ] `private`, `protected`, `public` method visibility
- [x] `attr_reader`, `attr_writer`, `attr_accessor`  COMPLETED
- [ ] Class variables `@@variable`
- [ ] Singleton methods `def obj.method`
- [ ] `super` with arguments
- **Impact**: Medium - Important for complex OOP
- **Complexity**: High - Method resolution complexity
- **Status**:  attr_* methods implemented and working

### File I/O & System
- [ ] `File.open`, `File.read`, `File.write`
- [ ] `Dir` class for directory operations
- [ ] `require` and `load` for file inclusion
- [ ] `$LOAD_PATH` management
- [ ] Command line argument handling (`ARGV`)
- **Impact**: Low-Medium - Utility features
- **Complexity**: Medium - System integration

### Advanced Data Types
- [ ] `Set` class for unique collections
- [ ] `Struct` for simple data objects
- [ ] `OpenStruct` for dynamic objects
- [ ] `Rational` and `Complex` numbers
- [ ] `Time` and `Date` classes
- **Impact**: Low - Convenience features
- **Complexity**: Medium - New class implementations

## 🔧 Infrastructure Improvements

### Performance & Optimization
- [ ] Tail call optimization
- [ ] Method call caching
- [ ] Constant folding in parser
- [ ] String interning for symbols
- [ ] Bytecode compilation (future)
- **Impact**: Medium - Performance improvements
- **Complexity**: High - Optimization complexity

### Developer Experience
- [ ] Better error messages with line numbers
- [ ] Stack traces for exceptions
- [ ] Interactive debugger (pry-like)
- [ ] Syntax highlighting in REPL
- [ ] Auto-completion in REPL
- **Impact**: Medium - Developer productivity
- **Complexity**: Medium-High - Tooling development

### Testing & Quality
- [ ] Built-in testing framework (minitest-like)
- [ ] Code coverage tools
- [ ] Static analysis tools
- [ ] Benchmarking utilities
- [ ] Memory profiling
- **Impact**: Low-Medium - Quality assurance
- **Complexity**: Medium-High - Tool development

## 🚨 Missing Basic Features (Discovered During Testing)

The following fundamental Ruby features are missing and cause examples to fail. These should be **HIGH PRIORITY** for implementation:

### 1. `.to_s` Method on Built-in Types
**Status**:  COMPLETED - Working on integers, symbols, booleans
**Examples**:
```ruby
# Integers
num = 42
puts(num.to_s)  # Output: "42"  WORKING

# Symbols
puts(:hello.to_s)  # Output: "hello"  WORKING

# Booleans
puts(true.to_s)  # Output: "true"  WORKING
```

### 2. `self` Method Calls
**Status**: ❌ Not implemented - `self.method_name` syntax fails
**Examples**:
```ruby
class Person
  attr_accessor :name

  def update_name(new_name)
    self.name = new_name  # Should work like: name = new_name
  end
end

# Current: Fails silently or with error
# Expected: Should call the setter method
```

### 3. Enhanced Arithmetic Operations
**Status**: ⚠️ Partially implemented - complex expressions may fail
**Examples**:
```ruby
# Integer division with parentheses
celsius = 100
fahrenheit = (celsius * 9 / 5) + 32  # Should be 212

# Modulo operator
remainder = 17 % 5  # Should be 2

# Exponentiation
power = 2 ** 3  # Should be 8
```

### 4. Comparison Operators
**Status**: ⚠️ Basic comparisons work, logical operators may fail
**Examples**:
```ruby
# Logical operators
x = true
y = false
puts(x && y)  # Should output: false
puts(x || y)  # Should output: true
puts(!x)      # Should output: false

# Combined conditions
if age >= 18 && age < 65
  puts("Working age")
end
```

### 5. Array and Hash Built-in Methods
**Status**:  PARTIALLY COMPLETED - Core array methods working
**Examples**:
```ruby
# Array methods  WORKING
arr = [1, 2, 3]
puts(arr.length)  # Output: 3  WORKING
puts(arr.first)   # Output: 1  WORKING
puts(arr.last)    # Output: 3  WORKING
puts(arr.empty?)  # Output: false  WORKING
arr[0] = 42       # Array assignment  WORKING

# Hash methods - Status unknown, needs testing
hash = {"name" => "Alice"}
puts(hash.keys)   # Needs testing
puts(hash.length) # Needs testing
```

## Implementation Notes

### Quick Wins (Good Starting Points)
1. **`.to_s` method** ✅ - High impact, low complexity - COMPLETED
2. **`self` method calls** - High impact, medium complexity
3. **String Interpolation** ✅ - High impact, manageable complexity - COMPLETED
4. **Ranges** ✅ - Fundamental feature, medium complexity - COMPLETED
5. **For/Until Loops** ✅ - Extends existing infrastructure - COMPLETED
6. **Constants & Symbols** ✅ - Core language features - PARTIALLY COMPLETED (symbols working)

### Major Undertakings
1. **Exception Handling** - Critical but complex
2. **Modules & Mixins** - High impact but very complex
3. **Regular Expressions** - Requires external library integration
4. **Metaprogramming** - Advanced features for later

### Dependencies
- **Ranges** → Enhanced case statements
- **Symbols** → Hash keys, metaprogramming
- **Modules** → Advanced OOP features
- **Exceptions** → Error handling throughout

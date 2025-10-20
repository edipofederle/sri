# SRI Ruby Spec Runner

A basic Ruby spec runner for testing the SRI (Small Ruby Interpreter) against the ruby/spec test suite.

## What We've Built

The spec runner (`src/sri/spec_runner.clj`) provides:

1. **Basic spec parsing** - Extracts simple assertions from Ruby spec files
2. **Expression evaluation** - Runs Ruby expressions through SRI 
3. **Result validation** - Compares actual vs expected results
4. **Progress reporting** - Shows passed/failed tests with details

## Usage

```bash
# Create a simple test spec
lein run -m sri.spec-runner create-test

# Run the simple test spec
lein run -m sri.spec-runner /Users/edipo/sri/test/simple_sri_spec.rb

# Run basic ruby/spec files (limited compatibility)  
lein run -m sri.spec-runner run-basic

# Run a specific spec file
lein run -m sri.spec-runner /path/to/spec_file.rb
```

## Current Results

### ✅ Working with SRI (5/5 tests pass)
- Basic arithmetic: `(1 + 2).should == 3`
- String length: `"hello".length.should == 5` 
- Array length: `[1, 2, 3].length.should == 3`
- Array indexing: `[1, 2, 3][0].should == 1`

### ❌ Ruby Spec Limitations Found

**Language Features Missing:**
- Constants (e.g., `Array`, `BasicObject`)
- Modules and includes (`Array.include?(Enumerable)`)
- Exception handling (`begin/rescue/end`)
- Splat operators (`*array`)
- Hash syntax in arrays (`[key => value]`)
- Percent literals (`%w(a b c)`)

**Parser Limitations:**
- No support for complex array literals with hashes
- No symbol syntax (`:symbol`)
- No splat operator parsing
- No percent notation parsing

**Method Resolution Issues:**
- String methods like `upcase` not working on raw string literals
- Some methods missing from built-in classes

## Validation Strategy Recommendations

### Phase 1: Current Capability Testing (Now)
Focus on specs that test SRI's existing features:
- Basic arithmetic and comparison operators
- String length and basic string methods
- Array creation, indexing, and length
- Hash creation and access
- Simple method calls and definitions

### Phase 2: Core Language Expansion (Next 2-3 months)
Implement missing features needed for broader spec compatibility:
1. **Constants** - `Array`, `String`, `Hash` class constants
2. **Exception handling** - `begin/rescue/ensure/end` blocks
3. **Modules** - `module`/`include` for mixins
4. **Symbol syntax** - `:symbol` literals
5. **Splat operators** - `*array` expansion

### Phase 3: Advanced Features (6+ months)
Add Ruby features for comprehensive spec compatibility:
- File I/O and `require` statements
- Regular expressions
- Advanced metaprogramming features
- Standard library classes

## Key Insights

1. **SRI is functional** - Core Ruby features work well for basic operations
2. **Spec infrastructure exists** - The ruby/spec suite provides comprehensive test coverage
3. **Gap analysis clear** - We can identify exactly what needs to be implemented
4. **Incremental validation** - We can test each new feature as it's added

## Next Steps

1. **Expand parser** - Add support for symbols, splat operators, percent literals
2. **Fix string methods** - Ensure string literals create RubyString objects
3. **Add constants** - Implement class constant lookup
4. **Implement modules** - Add basic module system for spec infrastructure
5. **Create targeted tests** - Build specific test suites for each Ruby feature as it's implemented

The spec runner provides a solid foundation for validating SRI's Ruby compatibility and identifying implementation priorities based on real Ruby specs.
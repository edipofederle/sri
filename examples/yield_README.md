# Yield Examples for Sri Ruby Interpreter

This directory contains comprehensive examples demonstrating the `yield` keyword functionality in the Sri Ruby interpreter.

## Examples Overview

### 1. `yield_basic.rb`
**Basic yield functionality examples**
- Simple yield with single argument
- Multiple yields in the same method
- Yield with multiple arguments
- Yield with return values

**Key concepts demonstrated:**
- How to define methods that accept blocks
- Using `yield` to execute passed blocks
- Capturing return values from yielded blocks

### 2. `yield_custom_iterators.rb`  
**Custom iterator implementations using yield**
- Custom `each` implementation
- Custom `each_with_index` implementation
- Custom `times` implementation
- Custom `map` implementation
- Custom `filter` implementation

**Key concepts demonstrated:**
- Building iterator methods from scratch
- Using yield to create reusable iteration patterns
- Implementing common Ruby enumerable methods

### 3. `yield_advanced.rb`
**Advanced yield patterns and techniques**
- Conditional yield execution
- Nested yield calls
- Method chaining with yield
- Generator patterns (Fibonacci sequence)

**Key concepts demonstrated:**
- Complex control flow with yield
- Yield in nested method contexts
- Building generator-style methods

### 4. `yield_error_cases.rb`
**Error handling and edge cases**
- Yield without block (LocalJumpError)
- Nested method calls with yield
- Different parameter counts

**Key concepts demonstrated:**
- Proper error handling when no block is provided
- Yield behavior in nested call stacks
- Flexible parameter handling in blocks

### 5. `yield_block_syntax.rb`
**Block syntax variations**
- Curly brace blocks `{ }`
- Do/end blocks
- Single-line vs multi-line block patterns

**Key concepts demonstrated:**
- When to use different block syntaxes
- Best practices for block formatting
- Compatibility between syntax styles

## How to Run Examples

```bash
# Run individual examples
lein run examples/yield_basic.rb
lein run examples/yield_custom_iterators.rb
lein run examples/yield_advanced.rb
lein run examples/yield_error_cases.rb
lein run examples/yield_block_syntax.rb
```

## Implementation Notes

The yield functionality in Sri is implemented with:

1. **Tokenizer support** - `yield` is recognized as a keyword
2. **Parser support** - `yield` is parsed like a method call with arguments
3. **Block context storage** - Method calls with blocks store block information in local variables
4. **Block execution** - `yield` retrieves and executes the stored block with provided arguments
5. **Error handling** - Proper Ruby-style LocalJumpError when yield is called without a block

## Expected Behavior

All examples include `# expected-output:` comments showing the exact output that should be produced when running each example. This makes it easy to verify correct implementation and behavior.

The yield implementation follows Ruby semantics:
- Blocks can accept zero, one, or multiple parameters
- Blocks can return values to the yielding method  
- Calling `yield` without a provided block raises a LocalJumpError
- Both `{}` and `do..end` block syntax work identically
- Yield works in nested method call contexts

## Integration with Existing Features

The yield implementation integrates seamlessly with:
- Existing block parsing (`{}` and `do..end` syntax)
- Method definition and calling mechanisms
- Variable scoping and parameter binding
- Error handling and exception propagation
- Built-in enumerable methods that already support blocks
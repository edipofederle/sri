# Demonstrates symbol literal support and methods


# expected-output: hello
# expected-output: world
# expected-output: ruby_symbol
# expected-output: test
# expected-output: example
# expected-output: foo
# expected-output: hello_world
# expected-output: bar
# expected-output: :test
# expected-output: :ruby
# expected-output: :inspect_me
# expected-output: 1
# expected-output: 5
# expected-output: 11
# expected-output: 4
# expected-output: 11
# expected-output: 15
# expected-output: 15
# expected-output: id2name_example
# expected-output: first
# expected-output: second
# expected-output: symbol_with_underscores
# expected-output: symbol123
# expected-output: _private_symbol
# expected-output: chainable
# expected-output: hello
# expected-output: hello
# expected-output: hello
# expected-output: puts
# expected-output: pending
# expected-output: completed
# expected-output: :database_url
# expected-output: Symbol: :demonstration
# expected-output: String: demonstration
# expected-output: Length: 13
# expected-output: Size: 13
# expected-output: ID2Name: demonstration


# Basic symbol literals
puts(:hello)                    # => :hello
puts(:world)                    # => :world
puts(:ruby_symbol)               # => :ruby_symbol

# Symbol assignment
sym = :test
puts(sym)                        # => :test

another_sym = :example
puts(another_sym)                # => :example

# Symbol methods - to_s (converts to string)
puts(:foo.to_s)                  # => foo
puts(:hello_world.to_s)          # => hello_world

sym = :bar
puts(sym.to_s)                   # => bar

# Symbol methods - inspect (returns Ruby representation)
puts(:test.inspect)              # => :test
puts(:ruby.inspect)              # => :ruby

sym = :inspect_me
puts(sym.inspect)                # => :inspect_me

# Symbol methods - length/size (returns character count)
puts(:a.length)                  # => 1
puts(:hello.length)              # => 5
puts(:hello_world.length)        # => 11

puts(:ruby.size)                 # => 4
puts(:programming.size)          # => 11

sym = :variable_symbol
puts(sym.length)                 # => 15
puts(sym.size)                   # => 15

# Symbol methods - empty? (checks if symbol name is empty)
# Note: In Ruby, symbols can't actually be empty, but the method exists
# puts(:"".empty?)               # Would be true if empty symbols were allowed

# Symbol methods - id2name (alias for to_s)
sym = :id2name_example
puts(sym.id2name)                # => id2name_example

# Symbols in expressions
sym1 = :first
sym2 = :second
puts(sym1)                       # => :first
puts(sym2)                       # => :second

# Symbols with underscores and numbers (valid Ruby symbol names)
puts(:symbol_with_underscores)  # => :symbol_with_underscores
puts(:symbol123)                 # => :symbol123
puts(:_private_symbol)           # => :_private_symbol

# Method chaining on symbols
sym = :chainable
result = sym.to_s
puts(result)                     # => chainable

# Symbols are commonly used as hash keys (when hashes are fully implemented)
# hash = {name: "Ruby", type: :language}
# puts(hash[:name])              # => Ruby
# puts(hash[:type])              # => :language

# Symbols vs Strings
# Symbols are immutable and unique - same symbol always refers to same object
# Strings are mutable and each string literal creates a new object
str = "hello"
sym = :hello
puts(str)                        # => hello
puts(sym)                        # => :hello
puts(sym.to_s)                   # => hello

# Common Ruby patterns with symbols
# Method names as symbols (useful for metaprogramming)
method_name = :puts
puts(method_name.to_s)           # => puts

# Status indicators
status = :pending
puts(status)                     # => :pending
status = :completed
puts(status)                     # => :completed

# Configuration keys
config_key = :database_url
puts(config_key.inspect)         # => :database_url

# Demonstrating all working symbol methods
test_symbol = :demonstration
puts("Symbol: " + test_symbol.inspect)           # => Symbol: :demonstration
puts("String: " + test_symbol.to_s)              # => String: demonstration
puts("Length: " + test_symbol.length.to_s)       # => Length: 13
puts("Size: " + test_symbol.size.to_s)           # => Size: 13
puts("ID2Name: " + test_symbol.id2name)          # => ID2Name: demonstration

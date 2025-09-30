 # Simple Symbol Examples with Expected Output

# expected-output: === Basic Symbol Literals ===
# expected-output: :hello
# expected-output: :world
# expected-output: === Symbol Assignment ===
# expected-output: :test
# expected-output: === Symbol Methods ===
# expected-output: foo
# expected-output: :bar
# expected-output: 5
# expected-output: === Symbol vs String ===
# expected-output: hello
# expected-output: :hello
# expected-output: hello

puts("=== Basic Symbol Literals ===")
puts(:hello)          # Expected: :hello
puts(:world)          # Expected: :world

puts("=== Symbol Assignment ===")
sym = :test
puts(sym)             # Expected: :test

puts("=== Symbol Methods ===")
puts(:foo.to_s)       # Expected: foo (string)
puts(:bar.inspect)    # Expected: :bar (with colon)
puts(:hello.length)   # Expected: 5

puts("=== Symbol vs String ===")
puts("hello")         # Expected: hello (string)
puts(:hello)          # Expected: :hello (symbol)
puts(:hello.to_s)     # Expected: hello (symbol converted to string)
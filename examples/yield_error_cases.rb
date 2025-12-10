# expected-output: === Yield Error Handling Examples ===
# expected-output: 
# expected-output: Testing yield without block will cause error:
# expected-output: About to call yield without block...
# expected-output: 
# expected-output: Testing yield in nested methods:
# expected-output: Outer method called with block
# expected-output: Inner method called, about to yield
# expected-output: Block executed with: 123
# expected-output: 
# expected-output: Testing yield with different parameter counts:
# expected-output: No params: block called
# expected-output: One param: received 42
# expected-output: Two params: 10 + 20 = 30

puts("=== Yield Error Handling Examples ===")
puts("")

# Yield without block - should error (commented out to not crash the example)
def method_without_block
  yield("hello")
end

puts("Testing yield without block will cause error:")
puts("About to call yield without block...")
# Uncomment the next line to see the error:
# method_without_block()

puts("")

# Nested method calls with yield
def outer_method_with_yield
  puts("Outer method called with block")
  inner_method_with_yield() { |x| puts("Block executed with: " + x.to_s) }
end

def inner_method_with_yield
  puts("Inner method called, about to yield")
  yield(123)
end

puts("Testing yield in nested methods:")
outer_method_with_yield() { |msg| puts("This won't be called") }

puts("")

# Testing different parameter counts
def test_params
  puts("No params: " + yield().to_s)
  puts("One param: " + yield(42).to_s)
  puts("Two params: " + yield(10, 20).to_s)
end

puts("Testing yield with different parameter counts:")
test_params() do |a, b|
  if a == nil
    "block called"
  else
    if b == nil
      "received " + a.to_s
    else
      a.to_s + " + " + b.to_s + " = " + (a + b).to_s
    end
  end
end
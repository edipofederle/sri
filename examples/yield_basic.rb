# expected-output: === Basic Yield Examples ===
# expected-output: 
# expected-output: Simple yield test:
# expected-output: Before yield
# expected-output: Block received: 42
# expected-output: After yield
# expected-output: 
# expected-output: Multiple yields:
# expected-output: Number: 1
# expected-output: Number: 2
# expected-output: Number: 3
# expected-output: 
# expected-output: Yield with multiple arguments:
# expected-output: 5 + 10 = 15
# expected-output: 12 + 18 = 30
# expected-output: 
# expected-output: Yield with return value:
# expected-output: Block returned: 25
# expected-output: Using return value: 50

puts("=== Basic Yield Examples ===")
puts("")

# Simple yield
def simple_yield_test
  puts("Before yield")
  yield(42)
  puts("After yield")
end

puts("Simple yield test:")
simple_yield_test() { |x| puts("Block received: " + x.to_s) }

puts("")

# Multiple yields
def multiple_yields
  yield(1)
  yield(2)
  yield(3)
end

puts("Multiple yields:")
multiple_yields() { |n| puts("Number: " + n.to_s) }

puts("")

# Yield with multiple arguments
def yield_multiple_args
  yield(5, 10)
  yield(12, 18)
end

puts("Yield with multiple arguments:")
yield_multiple_args() { |a, b| puts(a.to_s + " + " + b.to_s + " = " + (a + b).to_s) }

puts("")

# Yield with return value
def yield_with_return
  result = yield(5)
  puts("Block returned: " + result.to_s)
  return result * 2
end

puts("Yield with return value:")
final_result = yield_with_return() { |x| x * x }
puts("Using return value: " + final_result.to_s)
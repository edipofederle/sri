# expected-output: === Block Syntax Examples with Yield ===
# expected-output: 
# expected-output: Using curly brace blocks { }:
# expected-output: Square of 1: 1
# expected-output: Square of 2: 4
# expected-output: Square of 3: 9
# expected-output: 
# expected-output: Using do/end blocks:
# expected-output: Processing item 1: apple (length: 5)
# expected-output: Processing item 2: banana (length: 6)
# expected-output: Processing item 3: cherry (length: 6)
# expected-output: 
# expected-output: Single line vs multi-line blocks:
# expected-output: Quick: 10
# expected-output: Result: 10
# expected-output: Detailed processing of: 20
# expected-output: Step 1: validate input
# expected-output: Step 2: perform calculation
# expected-output: Step 3: return result
# expected-output: Result: 40

puts("=== Block Syntax Examples with Yield ===")
puts("")

# Method that works with any block syntax
def calculate_squares(numbers)
  i = 0
  while i < numbers.length
    square = yield(numbers[i])
    puts("Square of " + numbers[i].to_s + ": " + square.to_s)
    i = i + 1
  end
end

puts("Using curly brace blocks { }:")
nums = [1, 2, 3]
calculate_squares(nums) { |x| x * x }

puts("")

# Method for demonstrating do/end syntax
def process_items(items)
  i = 0
  while i < items.length
    yield(i + 1, items[i])
    i = i + 1
  end
end

puts("Using do/end blocks:")
items = ["apple", "banana", "cherry"]
process_items(items) do |index, item|
  length = item.length
  puts("Processing item " + index.to_s + ": " + item + " (length: " + length.to_s + ")")
end

puts("")

# Demonstrating when to use which syntax
def simple_or_complex(value)
  result = yield(value)
  puts("Result: " + result.to_s)
end

puts("Single line vs multi-line blocks:")

# Single line - use curly braces
simple_or_complex(10) { |x| puts("Quick: " + x.to_s); x }

# Multi-line - use do/end
simple_or_complex(20) do |x|
  puts("Detailed processing of: " + x.to_s)
  puts("Step 1: validate input")
  puts("Step 2: perform calculation")  
  puts("Step 3: return result")
  x * 2
end
# expected-output: Testing single-line blocks:
# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: 
# expected-output: Testing multi-line blocks:
# expected-output: Item: 1, Square: 1
# expected-output: Item: 2, Square: 4
# expected-output: Item: 3, Square: 9
# expected-output: 
# expected-output: Testing complex multi-line blocks:
# expected-output: Processing: 10
# expected-output: Result: 20
# expected-output: Processing: 20
# expected-output: Result: 40
# expected-output: Processing: 30
# expected-output: Result: 60
# expected-output: 
# expected-output: All tests completed!

# Test single-line blocks (should still work)
puts("Testing single-line blocks:")
numbers = [1, 2, 3]
numbers.each { |x| puts(x) }

puts("")

# Test multi-line blocks with simple expressions
puts("Testing multi-line blocks:")
numbers.each {
  |x|
  puts("Item: " + x.to_s() + ", Square: " + (x * x).to_s())
}

puts("")

# Test complex multi-line blocks with multiple statements
puts("Testing complex multi-line blocks:")
values = [10, 20, 30]
values.each {
  |value|
  puts("Processing: " + value.to_s())
  result = value * 2
  puts("Result: " + result.to_s())
}

puts("")
puts("All tests completed!")
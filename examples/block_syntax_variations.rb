# expected-output: Single-line block:
# expected-output: 1
# expected-output: 2
# expected-output: 
# expected-output: Multi-line block with parameter on same line:
# expected-output: Value: 1
# expected-output: Value: 2
# expected-output: 
# expected-output: Multi-line block with parameter on new line:
# expected-output: Processing 1
# expected-output: Processing 2
# expected-output: 
# expected-output: Multi-line block with multiple statements:
# expected-output: Starting with: 1
# expected-output: Doubled: 2
# expected-output: Starting with: 2
# expected-output: Doubled: 4

arr = [1, 2]

# Single-line block syntax
puts("Single-line block:")
arr.each { |x| puts(x) }

puts("")

# Multi-line block with parameter on same line as opening brace
puts("Multi-line block with parameter on same line:")
arr.each { |x|
  puts("Value: " + x.to_s())
}

puts("")

# Multi-line block with parameter on new line
puts("Multi-line block with parameter on new line:")
arr.each {
  |x|
  puts("Processing " + x.to_s())
}

puts("")

# Multi-line block with multiple statements
puts("Multi-line block with multiple statements:")
arr.each {
  |x|
  puts("Starting with: " + x.to_s())
  doubled = x * 2
  puts("Doubled: " + doubled.to_s())
}
# expected-output: Comparing { } and do/end block syntax:
# expected-output: 
# expected-output: Using { } blocks:
# expected-output: 2
# expected-output: 4
# expected-output: 6
# expected-output: 
# expected-output: Using do/end blocks:
# expected-output: Item 1: squared = 1
# expected-output: Item 2: squared = 4
# expected-output: Item 3: squared = 9
# expected-output: 
# expected-output: Multi-line do/end with string interpolation:
# expected-output: Processing 10: result is 20
# expected-output: Processing 20: result is 40
# expected-output: Processing 30: result is 60
# expected-output: 
# expected-output: Both syntaxes work identically!

puts("Comparing { } and do/end block syntax:")

puts("")

# Traditional { } block syntax  
arr = [1, 2, 3]
puts("Using { } blocks:")
arr.each { |x| puts(x * 2) }

puts("")

# New do/end block syntax
puts("Using do/end blocks:")
arr.each do |x|
  squared = x * x
  puts("Item #{x}: squared = #{squared}")
end

puts("")

# Multi-line do/end blocks with complex logic
values = [10, 20, 30]
puts("Multi-line do/end with string interpolation:")
values.each do |val|
  result = val * 2
  puts("Processing #{val}: result is #{result}")
end

puts("")
puts("Both syntaxes work identically!")
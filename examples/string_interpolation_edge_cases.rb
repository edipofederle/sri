# expected-output: Nested method calls:
# expected-output: Nested result: 100
# expected-output: 
# expected-output: String concatenation in interpolation:
# expected-output: Full name: John Doe
# expected-output: 
# expected-output: Comparison operations:
# expected-output: 5 > 3 is true
# expected-output: 2 == 2 is true
# expected-output: 
# expected-output: Boolean values:
# expected-output: Active: true
# expected-output: Disabled: false

# Nested method calls in interpolation
def multiply(x, y)
  return x * y
end

def add_ten(x)
  return x + 10
end

puts("Nested method calls:")
puts("Nested result: #{add_ten(multiply(9, 10))}")

puts("")

# String operations in interpolation
first_name = "John"
last_name = "Doe"
puts("String concatenation in interpolation:")
puts("Full name: #{first_name + " " + last_name}")

puts("")

# Comparison operations
puts("Comparison operations:")
puts("5 > 3 is #{5 > 3}")
puts("2 == 2 is #{2 == 2}")

puts("")

# Boolean values
active = true
disabled = false
puts("Boolean values:")
puts("Active: #{active}")
puts("Disabled: #{disabled}")
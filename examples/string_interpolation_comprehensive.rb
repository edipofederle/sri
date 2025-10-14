# expected-output: Basic variable interpolation:
# expected-output: Hello, Ruby!
# expected-output: Age: 25
# expected-output: 
# expected-output: Arithmetic in interpolation:
# expected-output: 5 + 3 = 8
# expected-output: 10 * 2 = 20
# expected-output: 
# expected-output: Array and method interpolation:
# expected-output: Array: [1 2 3]
# expected-output: First element: 1
# expected-output: Array length: 3
# expected-output: 
# expected-output: Method calls in interpolation:
# expected-output: Square of 6: 36
# expected-output: Complex calculation: 17
# expected-output: 
# expected-output: Multiple interpolations in one string:
# expected-output: User Alice is 30 years old and has score 95

# Basic variable interpolation
name = "Ruby"
age = 25
puts("Basic variable interpolation:")
puts("Hello, #{name}!")
puts("Age: #{age}")

puts("")

# Arithmetic expressions in interpolation
puts("Arithmetic in interpolation:")
puts("5 + 3 = #{5 + 3}")
puts("10 * 2 = #{10 * 2}")

puts("")

# Array and method interpolation
numbers = [1, 2, 3]
puts("Array and method interpolation:")
puts("Array: #{numbers.to_s()}")
puts("First element: #{numbers[0]}")
puts("Array length: #{numbers.length()}")

puts("")

# Method calls in interpolation
def square(x)
  return x * x
end

def calculate(a, b)
  return (a + b) * 2 + 1
end

puts("Method calls in interpolation:")
puts("Square of 6: #{square(6)}")
puts("Complex calculation: #{calculate(4, 4)}")

puts("")

# Multiple interpolations in one string
user_name = "Alice"
user_age = 30
score = 95
puts("Multiple interpolations in one string:")
puts("User #{user_name} is #{user_age} years old and has score #{score}")
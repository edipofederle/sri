# expected-output: Hello World!
# expected-output: Alice is 25 years old
# expected-output: The sum of 10 and 20 is 30
# expected-output: Product: 150, Division: 1
# expected-output: Temperature: 98.6°F (37°C)
# expected-output: Welcome Bob! Your account balance is $1500.75

# Basic variable interpolation
greeting = "Hello"
target = "World"
puts("#{greeting} #{target}!")

# Person information with interpolation
name = "Alice"
age = 25
puts("#{name} is #{age} years old")

# Mathematical expressions in interpolation
a = 10
b = 20
puts("The sum of #{a} and #{b} is #{a + b}")

# Multiple expressions in one string
x = 15
y = 10
puts("Product: #{x * y}, Division: #{x / y}")

# Mixed types - numbers and strings
temp_f = 98.6
temp_c = 37
puts("Temperature: #{temp_f}°F (#{temp_c}°C)")

# More complex example with method-like variables
user = "Bob"
balance = 1500.75
puts("Welcome #{user}! Your account balance is $#{balance}")

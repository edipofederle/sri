# expected-output: === String Methods Test ===
# expected-output: str: "Hello World"
# expected-output: upcase: "HELLO WORLD"
# expected-output: downcase: "hello world"
# expected-output: concatenation: "Hello World"
# expected-output: Methods test complete!

puts("=== String Methods Test ===")

# Test basic creation and methods
str = String.new("Hello World")
puts("str: " + str.inspect)
puts("upcase: " + str.upcase.inspect)
puts("downcase: " + str.downcase.inspect)

# Test concatenation
str1 = String.new("Hello")
str2 = String.new(" World")
result = str1 + str2
puts("concatenation: " + result.inspect)

puts("Methods test complete!")

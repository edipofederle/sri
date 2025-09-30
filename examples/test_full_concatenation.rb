# expected-output: Testing full string concatenation:
# expected-output: Hello: 1

puts("Testing full string concatenation:")
puts("Hello: " + [1, 2, 3][0].to_s)
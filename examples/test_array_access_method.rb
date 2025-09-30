# expected-output: Testing array access with methods:
# expected-output: Direct array access:
# expected-output: 1
# expected-output: Array access with to_s:
# expected-output: 1
# expected-output: String concatenation:
# expected-output:Hello: 1

puts("Testing array access with methods:")
puts("Direct array access:")
puts([1, 2, 3][0])

puts("Array access with to_s:")
puts([1, 2, 3][0].to_s)

puts("String concatenation:")
puts("Hello: " + [1, 2, 3][0].to_s)

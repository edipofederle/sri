# expected-output: Direct literal:
# expected-output: 1
# expected-output: Variable access:
# expected-output: 1

puts("Direct literal:")
puts([1, 2, 3][0].to_s)

puts("Variable access:")
numbers = [1, 2, 3]
puts(numbers[0].to_s)
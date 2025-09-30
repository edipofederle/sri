# expected-output: Original array:
# expected-output: numbers[0] = 10
# expected-output: numbers[1] = 20
# expected-output: numbers[4] = 50
# expected-output: After assignments:
# expected-output: numbers[0] = 100
# expected-output: numbers[1] = 200
# expected-output: numbers[4] = 500

numbers = [10, 20, 30, 40, 50]

puts("Original array:")
puts("numbers[0] = " + numbers[0].to_s)
puts("numbers[1] = " + numbers[1].to_s)
puts("numbers[4] = " + numbers[4].to_s)

numbers[0] = 100
numbers[1] = 200
numbers[4] = 500

puts("After assignments:")
puts("numbers[0] = " + numbers[0].to_s)
puts("numbers[1] = " + numbers[1].to_s)
puts("numbers[4] = " + numbers[4].to_s)

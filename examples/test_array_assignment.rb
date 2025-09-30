# expected-output: Before assignment:
# expected-output: 1
# expected-output: After assignment:
# expected-output: 42

numbers = [1, 2, 3]
puts("Before assignment:")
puts(numbers[0])

numbers[0] = 42
puts("After assignment:")
puts(numbers[0])
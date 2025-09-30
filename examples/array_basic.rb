#=== Array Basic Operations ===

# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: 4
# expected-output: 5
# expected-output: 1
# expected-output: Second element: 2
# expected-output: Last element: 5
# expected-output: Last via negative index: 5
# expected-output: Second to last: 4
# expected-output: After setting first element to 42:
# expected-output: 42
# expected-output: 2
# expected-output: 3
# expected-output: 4
# expected-output: 5
# expected-output: ---
# expected-output: After setting index 7 to 99 (expanding array):
# expected-output: 42
# expected-output: 2
# expected-output: 3
# expected-output: 4
# expected-output: 5
# expected-output: 
# expected-output: 
# expected-output: 99
# expected-output: Array length: 8
# expected-output: First element: 42
# expected-output: Last element: 99
# expected-output: Is empty array empty? true
# expected-output: Is numbers array empty? false


# Create empty array
empty_array = []

# # Create array with elements
numbers = [1, 2, 3, 4, 5]
puts(numbers)

# # Array access with pose
puts(numbers[0])

puts("Second element: " + numbers[1].to_s)
puts("Last element: " + numbers[4].to_s)
puts("Last via negative index: " + numbers[-1].to_s)
puts("Second to last: " + numbers[-2].to_s)

numbers[0] = 42
puts("After setting first element to 42:")
puts(numbers)
puts("---")

numbers[7] = 99
puts("After setting index 7 to 99 (expanding array):")
puts(numbers)

puts("Array length: " + numbers.length.to_s)

puts("First element: " + numbers.first.to_s)
puts("Last element: " + numbers.last.to_s)

puts("Is empty array empty? " + empty_array.empty?.to_s)
puts("Is numbers array empty? " + numbers.empty?.to_s)

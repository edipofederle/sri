# expected-output: === Array containing range ===
# expected-output: Array: [1...10]
# expected-output:
# expected-output: === Using each on array with range ===
# expected-output: 1...10
# expected-output:
# expected-output: === Expected behavior comparison ===
# expected-output: Direct range iteration:
# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: 4
# expected-output: 5
# expected-output: 6
# expected-output: 7
# expected-output: 8
# expected-output: 9
# expected-output:
# expected-output: Regular array iteration:
# expected-output: 1
# expected-output: 2
# expected-output: 3

puts("=== Array containing range ===")
arr = [1...10]
puts("Array: " + arr.to_s)

puts("\n=== Using each on array with range ===")
arr.each { |x| puts(x) }

puts("\n=== Expected behavior comparison ===")
puts("Direct range iteration:")
(1...10).each { |x| puts(x) }

puts("\nRegular array iteration:")
normal_arr = [1, 2, 3]
normal_arr.each { |x| puts(x) }

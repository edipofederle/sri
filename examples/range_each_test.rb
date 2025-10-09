# expected-output: === Numeric Range Each ===
# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: 4
# expected-output: 5
# expected-output:
# expected-output: === Character Range Each ===
# expected-output: a
# expected-output: b
# expected-output: c
# expected-output: d
# expected-output: e
# expected-output:
# expected-output: === Array with Range Each ===
# expected-output: Array: [1..3]
# expected-output: Array each:
# expected-output: 1..3
# expected-output:
# expected-output: === Direct Range vs Array with Range ===
# expected-output: Direct range each:
# expected-output: 10
# expected-output: 11
# expected-output: 12
# expected-output: Array with range each:
# expected-output: 10..12

puts("=== Numeric Range Each ===")
(1..5).each { |x| puts(x) }

puts("\n=== Character Range Each ===")
("a".."e").each { |c| puts(c) }

puts("\n=== Array with Range Each ===")
arr = [1..3]
puts("Array: " + arr.to_s)
puts("Array each:")
arr.each { |item| puts(item) }

puts("\n=== Direct Range vs Array with Range ===")
puts("Direct range each:")
(10..12).each { |x| puts(x) }

puts("Array with range each:")
arr2 = [10..12]
arr2.each { |range| puts(range) }

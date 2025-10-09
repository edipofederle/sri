# Test Ruby ranges implementation

# expected-output: === Range Creation Tests ===
# expected-output: Inclusive range 1..5: 1..5
# expected-output: Exclusive range 1...5: 1...5
# expected-output:
# expected-output: === Range Methods Tests ===
# expected-output: 1..5 to_a: [1 2 3 4 5]
# expected-output: 1...5 to_a: [1 2 3 4]
# expected-output: 1..5 include 3: true
# expected-output: 1..5 include 5: true
# expected-output: 1...5 include 5: false
# expected-output: 1..5 include 6: false
# expected-output: 1..5 size: 5
# expected-output: 1...5 size: 4
# expected-output: 1..5 count: 5
# expected-output:
# expected-output: === Range in Case Statements ===
# expected-output: number is between 1 and 3 (inclusive)
# expected-output: number is between 4 and 7 (exclusive)
# expected-output: number is out of range
# expected-output:
# expected-output: === Different Range Types ===
# expected-output: 0..2 to_a: [0 1 2]
# expected-output: -2..2 to_a: [-2 -1 0 1 2]
# expected-output: 5..5 to_a: [5]
# expected-output: 5..5 size: 1
# expected-output: 5...5 to_a: []
# expected-output: 5...5 size: 0
# expected-output:
# expected-output: Range tests complete!

puts("=== Range Creation Tests ===")

# Inclusive ranges
r1 = 1..5
puts("Inclusive range 1..5: " + r1.to_s)

# Exclusive ranges
r2 = 1...5
puts("Exclusive range 1...5: " + r2.to_s)

puts("\n=== Range Methods Tests ===")

puts("1..5 to_a: " + r1.to_a.to_s)
puts("1...5 to_a: " + r2.to_a.to_s)

puts("1..5 include 3: " + r1.include?(3).to_s)
puts("1..5 include 5: " + r1.include?(5).to_s)
puts("1...5 include 5: " + r2.include?(5).to_s)
puts("1..5 include 6: " + r1.include?(6).to_s)

puts("1..5 size: " + r1.size.to_s)
puts("1...5 size: " + r2.size.to_s)
puts("1..5 count: " + r1.count.to_s)

puts("\n=== Range in Case Statements ===")

number = 3
case number
when 1..3
  puts("number is between 1 and 3 (inclusive)")
when 4...8
  puts("number is between 4 and 7 (exclusive)")
else
  puts("number is out of range")
end

number = 7
case number
when 1..3
  puts("number is between 1 and 3 (inclusive)")
when 4...8
  puts("number is between 4 and 7 (exclusive)")
else
  puts("number is out of range")
end

number = 8
case number
when 1..3
  puts("number is between 1 and 3 (inclusive)")
when 4...8
  puts("number is between 4 and 7 (exclusive)")
else
  puts("number is out of range")
end

puts("\n=== Different Range Types ===")

zero_range = 0..2
puts("0..2 to_a: " + zero_range.to_a.to_s)

neg_range = -2..2
puts("-2..2 to_a: " + neg_range.to_a.to_s)

single = 5..5
puts("5..5 to_a: " + single.to_a.to_s)
puts("5..5 size: " + single.size.to_s)

empty = 5...5
puts("5...5 to_a: " + empty.to_a.to_s)
puts("5...5 size: " + empty.size.to_s)

puts("\nRange tests complete!")

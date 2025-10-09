# Test character ranges implementation

# expected-output: === Character Range Creation Tests ===
# expected-output: Character range a..z: "a".."z"
# expected-output: Character range A...Z: "A"..."Z"
# expected-output: Character range x..z: "x".."z"
# expected-output:
# expected-output: === Character Range Methods Tests ===
# expected-output: a..e to_a: ["a" "b" "c" "d" "e"]
# expected-output: x...z to_a: ["x" "y"]
# expected-output: a..z include m: true
# expected-output: a..z include z: true
# expected-output: a..z include A: false
# expected-output: a..z include 5: false
# expected-output: a..z size: 26
# expected-output: A...Z size: 25
# expected-output: x..z count: 3
# expected-output:
# expected-output: === Character Range in Case Statements ===
# expected-output: letter is in first half of alphabet
# expected-output: uppercase letter
# expected-output:
# expected-output: === Mixed Character and Number Ranges ===
# expected-output: Mixed ranges array:
# expected-output: 1..5
# expected-output: "a".."e"
# expected-output: 10...15
# expected-output: "x"..."z"
# expected-output:
# expected-output: === Edge Cases ===
# expected-output: a..a to_a: ["a"]
# expected-output: a..a size: 1
# expected-output: a...a to_a: []
# expected-output: a...a size: 0
# expected-output: 0..9 to_a: ["0" "1" "2" "3" "4" "5" "6" "7" "8" "9"]
# expected-output: 0..9 size: 10
# expected-output:
# expected-output: Character range tests complete!

puts("=== Character Range Creation Tests ===")

# Lowercase character ranges
char_range1 = "a".."z"
puts("Character range a..z: " + char_range1.to_s)

# Uppercase character ranges
char_range2 = "A"..."Z"
puts("Character range A...Z: " + char_range2.to_s)

# Small character ranges
char_range3 = "x".."z"
puts("Character range x..z: " + char_range3.to_s)

puts("\n=== Character Range Methods Tests ===")

# to_a method
puts("a..e to_a: " + ("a".."e").to_a.to_s)
puts("x...z to_a: " + ("x"..."z").to_a.to_s)

# include? method
alphabet = "a".."z"
puts("a..z include m: " + alphabet.include?("m").to_s)
puts("a..z include z: " + alphabet.include?("z").to_s)
puts("a..z include A: " + alphabet.include?("A").to_s)
puts("a..z include 5: " + alphabet.include?("5").to_s)

# size/count methods
puts("a..z size: " + alphabet.size.to_s)
puts("A...Z size: " + ("A"..."Z").size.to_s)
puts("x..z count: " + ("x".."z").count.to_s)

puts("\n=== Character Range in Case Statements ===")

letter = "m"
case letter
when "a".."m"
  puts("letter is in first half of alphabet")
when "n".."z"
  puts("letter is in second half of alphabet")
else
  puts("letter is not a lowercase letter")
end

letter = "Z"
case letter
when "a".."z"
  puts("lowercase letter")
when "A".."Z"
  puts("uppercase letter")
else
  puts("not a letter")
end

puts("\n=== Mixed Character and Number Ranges ===")

# Arrays with mixed ranges
mixed_ranges = [1..5, "a".."e", 10...15, "x"..."z"]
puts("Mixed ranges array:")
puts(mixed_ranges)

puts("\n=== Edge Cases ===")

# Single character range
single_char = "a".."a"
puts("a..a to_a: " + single_char.to_a.to_s)
puts("a..a size: " + single_char.size.to_s)

# Empty character range
empty_char = "a"..."a"
puts("a...a to_a: " + empty_char.to_a.to_s)
puts("a...a size: " + empty_char.size.to_s)

# Numbers as strings (should work as character ranges if single char)
num_char = "0".."9"
puts("0..9 to_a: " + num_char.to_a.to_s)
puts("0..9 size: " + num_char.size.to_s)

puts("\nCharacter range tests complete!")

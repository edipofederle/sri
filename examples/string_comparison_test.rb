# expected-output: === String Comparison Test ===
# expected-output: str_a: "apple"
# expected-output: str_b: "banana"
# expected-output: str_c: "apple"
# expected-output: str_a == str_c: true
# expected-output: str_a == str_b: false
# expected-output: Comparison test complete!

puts("=== String Comparison Test ===")

str_a = String.new("apple")
str_b = String.new("banana")
str_c = String.new("apple")

puts("str_a: " + str_a.inspect)
puts("str_b: " + str_b.inspect)
puts("str_c: " + str_c.inspect)

puts("str_a == str_c: " + (str_a == str_c).to_s)
puts("str_a == str_b: " + (str_a == str_b).to_s)

puts("Comparison test complete!")

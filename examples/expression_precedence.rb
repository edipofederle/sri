# Test operator precedence edge cases
# expected-output: 14
# expected-output: 20
# expected-output: 7
# expected-output: 1

puts(2 + 3 * 4)      # Should be 14 (multiplication first)
puts((2 + 3) * 4)    # Should be 20 (parentheses first)
puts(10 - 2 - 1)     # Should be 7 (left to right)
puts(8 / 4 / 2)      # Should be 1 (left to right)
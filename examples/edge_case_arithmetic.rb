# Test edge cases in arithmetic operations
# expected-output: 0
# expected-output: 1
# expected-output: -5
# expected-output: 25

# Division edge cases
puts(5 / 5 - 1)  # Should be 0

# Negative number operations  
puts(-2 + 3)     # Should be 1

# Multiple operations in one expression
puts(10 - 5 * 3) # Should be -5 (order of operations)

# Simple multiplication
puts(5 * 5)      # Should be 25
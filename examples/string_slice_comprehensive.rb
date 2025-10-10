# Comprehensive slice method test
puts("=== String Slice Comprehensive Test ===")

text = String.new("Hello Ruby World")
puts("text: " + text.inspect)

# Test single character access
puts("\n--- Single Character Access ---")
puts("text.slice(0): " + text.slice(0).inspect)
puts("text.slice(6): " + text.slice(6).inspect)
puts("text.slice(-1): " + text.slice(-1).inspect)  # Should be nil for negative
puts("text.slice(100): " + text.slice(100).inspect)  # Should be nil for out of bounds

# Test substring access
puts("\n--- Substring Access ---")
puts("text.slice(0, 5): " + text.slice(0, 5).inspect)
puts("text.slice(6, 4): " + text.slice(6, 4).inspect)
puts("text.slice(11, 5): " + text.slice(11, 5).inspect)
puts("text.slice(6, 0): " + text.slice(6, 0).inspect)  # Zero length
puts("text.slice(100, 5): " + text.slice(100, 5).inspect)  # Out of bounds

# Test edge cases
puts("\n--- Edge Cases ---")
empty_str = String.new("")
puts("empty_str.slice(0): " + empty_str.slice(0).inspect)
puts("empty_str.slice(0, 1): " + empty_str.slice(0, 1).inspect)

single_char = String.new("X")
puts("single_char.slice(0): " + single_char.slice(0).inspect)
puts("single_char.slice(0, 1): " + single_char.slice(0, 1).inspect)

puts("\nSlice comprehensive test complete!")
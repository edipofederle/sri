# Test simple enumerable methods one by one
puts("=== Simple Enumerable Test ===")

puts("\n--- Collect ---")
result = (1..3).collect { |x| x * x }
puts("Collect: " + result.to_s)

puts("\n--- Filter ---")
result = [2, 4, 6].filter { |x| x > 3 }
puts("Filter: " + result.to_s)

puts("\n--- Detect ---")
result = (1..5).detect { |x| x > 3 }
puts("Detect: " + result.to_s)
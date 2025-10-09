# Test advanced enumerable methods
puts("=== Advanced Enumerable Methods ===")

puts("\n--- Collect (alias for map) ---")
result = (1..3).collect { |x| x * x }
puts("Collect result: " + result.to_s)

puts("\n--- Filter (alias for select) ---")
result = [1, 2, 3, 4, 5].filter { |x| x % 2 == 0 }
puts("Filter result: " + result.to_s)

puts("\n--- Detect (alias for find) ---")
result = ("a".."z").detect { |c| c == "m" }
puts("Detect result: " + result.to_s)

puts("\n--- Character range select ---")
result = ("a".."e").select { |c| c == "a" || c == "e" }
puts("A and E: " + result.to_s)

puts("\n--- Range any? and all? ---")
puts("Any number > 5 in 1..10: " + (1..10).any? { |x| x > 5 }.to_s)
puts("All numbers > 0 in 1..5: " + (1..5).all? { |x| x > 0 }.to_s)
puts("All numbers > 3 in 1..5: " + (1..5).all? { |x| x > 3 }.to_s)

puts("\nAdvanced enumerable tests complete!")
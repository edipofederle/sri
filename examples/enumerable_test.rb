# Test enumerable functionality across arrays and ranges
puts("=== Enumerable Methods Test ===")

puts("\n--- Array Each ---")
[1, 2, 3].each { |x| puts(x) }

puts("\n--- Range Each ---") 
(1..3).each { |x| puts(x) }

puts("\n--- Character Range Each ---")
("a".."c").each { |c| puts(c) }

puts("\n--- Array Map ---")
result1 = [1, 2, 3].map { |x| x * 2 }
puts("Map result: " + result1.to_s)

puts("\n--- Range Map ---")
result2 = (1..3).map { |x| x * 2 }
puts("Range map result: " + result2.to_s)

puts("\n--- Array Select ---")
result3 = [1, 2, 3, 4, 5].select { |x| x > 3 }
puts("Select result: " + result3.to_s)

puts("\n--- Range Select ---")
result4 = (1..5).select { |x| x > 3 }
puts("Range select result: " + result4.to_s)

puts("\n--- Array Reject ---")
result5 = [1, 2, 3, 4, 5].reject { |x| x > 3 }
puts("Reject result: " + result5.to_s)

puts("\n--- Range Find ---")
result6 = (1..10).find { |x| x > 5 }
puts("Find result: " + result6.to_s)

puts("\n--- Array Any? ---")
result7 = [1, 2, 3].any? { |x| x > 2 }
puts("Any? result: " + result7.to_s)

puts("\n--- Range All? ---")
result8 = (1..3).all? { |x| x > 0 }
puts("All? result: " + result8.to_s)

puts("\nEnumerable tests complete!")
# expected-output: === String Query Test ===
# expected-output: text: "Hello Ruby World"
# expected-output: text.start_with?("Hello"): true
# expected-output: text.end_with?("World"): true
# expected-output: text.include?("Ruby"): true
# expected-output: Query test complete!

puts("=== String Query Test ===")

text = String.new("Hello Ruby World")
puts("text: " + text.inspect)

puts("text.start_with?(\"Hello\"): " + text.start_with?("Hello").to_s)
puts("text.end_with?(\"World\"): " + text.end_with?("World").to_s)
puts("text.include?(\"Ruby\"): " + text.include?("Ruby").to_s)

puts("Query test complete!")

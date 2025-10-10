# expected-output: === Basic String Indexing Test ===
# expected-output: text: "Hello"
# expected-output: text.index("e"): 1
# expected-output: Basic indexing test complete!
puts("=== Basic String Indexing Test ===")

text = String.new("Hello")
puts("text: " + text.inspect)

# Test basic index method first
puts("text.index(\"e\"): " + text.index("e").to_s)

puts("Basic indexing test complete!")

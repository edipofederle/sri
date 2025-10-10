# Test String indexing methods
puts("=== String Indexing Test ===")

text = String.new("Hello Ruby World")
puts("text: " + text.inspect)

puts("text.index(\"Ruby\"): " + text.index("Ruby").to_s)
puts("text.index(\"Python\"): " + text.index("Python").inspect)

# Test array access
puts("text[0]: " + text[0].inspect)
puts("text[6, 4]: " + text[6, 4].inspect)

puts("Indexing test complete!")
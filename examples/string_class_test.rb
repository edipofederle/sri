# Test Ruby String class functionality
puts("=== Ruby String Class Test ===")

# Test String creation
puts("\n--- String Creation ---")
str1 = String.new
str2 = String.new("Hello, World!")
puts("Empty string: " + str1.inspect)
puts("String with value: " + str2.inspect)

# Test basic methods
puts("\n--- Basic Methods ---")
puts("str2.to_s: " + str2.to_s)
puts("str2.class: " + str2.class)
puts("str2.length: " + str2.length.to_s)
puts("str2.size: " + str2.size.to_s)
puts("str1.empty?: " + str1.empty?.to_s)
puts("str2.empty?: " + str2.empty?.to_s)

# Test case methods
puts("\n--- Case Methods ---")
puts("str2.upcase: " + str2.upcase.inspect)
puts("str2.downcase: " + str2.downcase.inspect)
puts("str2.capitalize: " + str2.capitalize.inspect)

# Test manipulation methods
puts("\n--- Manipulation Methods ---")
puts("str2.reverse: " + str2.reverse.inspect)
test_str = String.new("  hello world  ")
puts("test_str: " + test_str.inspect)
puts("test_str.strip: " + test_str.strip.inspect)

# Test concatenation
puts("\n--- Concatenation ---")
hello = String.new("Hello")
world = String.new(" World")
combined = hello + world
puts("hello: " + hello.inspect)
puts("world: " + world.inspect)
puts("hello + world: " + combined.inspect)

# Test comparison
puts("\n--- Comparison ---")
str_a = String.new("apple")
str_b = String.new("banana")
str_c = String.new("apple")
puts("str_a: " + str_a.inspect)
puts("str_b: " + str_b.inspect)
puts("str_c: " + str_c.inspect)
puts("str_a == str_c: " + (str_a == str_c).to_s)
puts("str_a == str_b: " + (str_a == str_b).to_s)
puts("str_a < str_b: " + (str_a < str_b).to_s)
puts("str_b > str_a: " + (str_b > str_a).to_s)

# Test query methods
puts("\n--- Query Methods ---")
text = String.new("Hello Ruby World")
puts("text: " + text.inspect)
puts("text.start_with?(\"Hello\"): " + text.start_with?("Hello").to_s)
puts("text.end_with?(\"World\"): " + text.end_with?("World").to_s)
puts("text.include?(\"Ruby\"): " + text.include?("Ruby").to_s)
puts("text.include?(\"Python\"): " + text.include?("Python").to_s)

# Test indexing
puts("\n--- Indexing ---")
puts("text.index(\"Ruby\"): " + text.index("Ruby").to_s)
puts("text.index(\"Python\"): " + text.index("Python").inspect)
puts("text[0]: " + text[0].inspect)
puts("text[6, 4]: " + text[6, 4].inspect)

# Test string manipulation
puts("\n--- String Manipulation ---")
test_line = String.new("Hello World\n")
puts("test_line: " + test_line.inspect)
puts("test_line.chomp: " + test_line.chomp.inspect)
puts("test_line.chop: " + test_line.chop.inspect)

# Test splitting
puts("\n--- String Splitting ---")
csv = String.new("apple,banana,cherry")
puts("csv: " + csv.inspect)
parts = csv.split(",")
puts("csv.split(\",\"): " + parts.inspect)

# Test introspection
puts("\n--- Introspection ---")
puts("str2.respond_to?(:upcase): " + str2.respond_to?(:upcase).to_s)
puts("str2.respond_to?(:nonexistent): " + str2.respond_to?(:nonexistent).to_s)
puts("str2.instance_of?(\"String\"): " + str2.instance_of?("String").to_s)
puts("str2.kind_of?(\"Object\"): " + str2.kind_of?("Object").to_s)

puts("\nString class test complete!")
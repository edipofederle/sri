# expected-output: === Ruby Kernel Methods Test ===
# expected-output:
# expected-output: --- Object.puts ---
# expected-output: Hello from object!
# expected-output: Line 1
# expected-output: Line 2
# expected-output: Line 3
# expected-output:
# expected-output: --- Object.p ---
# expected-output: "debugging" 42 true
# expected-output: "single value"
# expected-output: p returned: "single value"
# expected-output:
# expected-output: --- Object.print ---
# expected-output: No newline here!
# expected-output:
# expected-output: Kernel methods test complete!

puts("=== Ruby Kernel Methods Test ===")

# Test puts method on objects
puts("\n--- Object.puts ---")
obj = Object.new("test object")
obj.puts("Hello from object!")
obj.puts("Line 1", "Line 2", "Line 3")

# Test p method on objects
puts("\n--- Object.p ---")
obj.p("debugging", 42, true)
result = obj.p("single value")
puts("p returned: " + result.inspect)

# Test print method on objects
puts("\n--- Object.print ---")
obj.print("No newline ")
obj.print("here!")
puts("")  # Add newline

puts("\nKernel methods test complete!")

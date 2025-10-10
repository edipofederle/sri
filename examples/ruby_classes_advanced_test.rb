# Test advanced Ruby class hierarchy functionality
puts("=== Ruby Classes Advanced Test ===")

# Test object creation with arguments
puts("\n--- Object Creation ---")
obj1 = Object.new
obj2 = Object.new("hello world")
puts("Empty object: " + obj1.inspect)
puts("Object with value: " + obj2.inspect)

# Test method calls
puts("\n--- Method Calls ---")
puts("obj1.to_s: " + obj1.to_s)
puts("obj2.to_s: " + obj2.to_s)
puts("obj1.class: " + obj1.class)
puts("obj2.class: " + obj2.class)

# Test introspection
puts("\n--- Introspection ---")
puts("obj1.respond_to?(:to_s): " + obj1.respond_to?(:to_s).to_s)
puts("obj1.respond_to?(:class): " + obj1.respond_to?(:class).to_s)
puts("obj1.respond_to?(:nonexistent): " + obj1.respond_to?(:nonexistent).to_s)

# Test nil checking
puts("\n--- Nil Checking ---")
nil_obj = Object.new(nil)
str_obj = Object.new("test")
puts("nil_obj.nil?: " + nil_obj.nil?.to_s)
puts("str_obj.nil?: " + str_obj.nil?.to_s)

# Test equality
puts("\n--- Equality ---")
obj3 = Object.new("test")
obj4 = Object.new("test")
puts("obj3 == obj4 (same value): " + (obj3 == obj4).to_s)
puts("obj3.equal?(obj4) (same object): " + obj3.equal?(obj4).to_s)
puts("obj3.equal?(obj3) (same object): " + obj3.equal?(obj3).to_s)

# Test object_id
puts("\n--- Object ID ---")
puts("obj1.object_id: " + obj1.object_id.to_s)
puts("obj2.object_id: " + obj2.object_id.to_s)

puts("\nAdvanced tests complete!")
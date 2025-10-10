# Test Ruby class hierarchy core infrastructure
puts("=== Ruby Classes Core Infrastructure Test ===")

# Test BasicObject creation and methods
puts("\n--- BasicObject Tests ---")
basic_obj = BasicObject.new
puts("BasicObject class: " + basic_obj.class.to_s)
puts("BasicObject ancestors: " + basic_obj.class.ancestors.to_s)
puts("BasicObject to_s: " + basic_obj.to_s)
puts("BasicObject inspect: " + basic_obj.inspect)

# Test Object creation and methods  
puts("\n--- Object Tests ---")
obj = Object.new
puts("Object class: " + obj.class.to_s)
puts("Object ancestors: " + obj.class.ancestors.to_s)
puts("Object to_s: " + obj.to_s)
puts("Object inspect: " + obj.inspect)

# Test Object with value
puts("\n--- Object with Value Tests ---")
obj_with_value = Object.new("hello")
puts("Object with value to_s: " + obj_with_value.to_s)
puts("Object with value inspect: " + obj_with_value.inspect)

# Test method lookup and respond_to?
puts("\n--- Method Lookup Tests ---")
puts("Object responds to to_s: " + obj.respond_to?(:to_s).to_s)
puts("Object responds to class: " + obj.respond_to?(:class).to_s)
puts("Object responds to nonexistent: " + obj.respond_to?(:nonexistent).to_s)

# Test nil, true, false objects
puts("\n--- Ruby Literal Objects ---")
nil_obj = Object.new(nil)
puts("Nil object: " + nil_obj.inspect)
puts("Nil object nil?: " + nil_obj.nil?.to_s)

true_obj = Object.new(true)
puts("True object: " + true_obj.inspect)
puts("True object nil?: " + true_obj.nil?.to_s)

false_obj = Object.new(false)
puts("False object: " + false_obj.inspect)
puts("False object nil?: " + false_obj.nil?.to_s)

# Test equality
puts("\n--- Equality Tests ---")
obj1 = Object.new("test")
obj2 = Object.new("test")
obj3 = obj1
puts("obj1 == obj2 (same value): " + (obj1 == obj2).to_s)
puts("obj1.equal?(obj2) (same object): " + obj1.equal?(obj2).to_s)
puts("obj1.equal?(obj3) (same object): " + obj1.equal?(obj3).to_s)

puts("\nCore infrastructure tests complete!")
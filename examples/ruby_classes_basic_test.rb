puts("=== Ruby Classes Basic Test ===")

# Test creating Ruby objects
obj = Object.new
puts("Object created: " + obj.inspect)
puts("Object class: " + obj.class)

# Test basic methods
puts("Object responds to to_s: " + obj.respond_to?(:to_s).to_s)
puts("Object responds to class: " + obj.respond_to?(:class).to_s)

# Test nil, true, false
nil_obj = Object.new(nil)
puts("Nil object: " + nil_obj.inspect)
puts("Nil?: " + nil_obj.nil?.to_s)

true_obj = Object.new(true)
puts("True object: " + true_obj.inspect)

false_obj = Object.new(false)
puts("False object: " + false_obj.inspect)

puts("Test complete!")

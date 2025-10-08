class TestClass
  attr_accessor :value
  
  def initialize(v)
    @value = v
  end
  
  def double_value
    @value * 2
  end
end

# Create instances
obj1 = TestClass.new(10)
obj2 = TestClass.new(20)

puts("First round of method calls:")
puts(obj1.value)          # IC MISS - first call
puts(obj1.double_value()) # IC MISS - first call

puts("Second round (should hit IC):")
puts(obj1.value)          # IC HIT - same class, same method
puts(obj1.double_value()) # IC HIT - same class, same method

puts("Third round with second object:")
puts(obj2.value)          # IC HIT - same class, same method  
puts(obj2.double_value()) # IC HIT - same class, same method

puts("\nTest completed - IC should be in monomorphic state")
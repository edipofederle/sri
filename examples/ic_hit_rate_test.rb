class TestObj
  attr_accessor :value
  
  def initialize(v)
    @value = v
  end
  
  def double
    @value * 2
  end
end

obj = TestObj.new(5)

puts("=== Testing hit rates ===")
puts("First call (miss expected):")
puts(obj.value)

puts("Second call (hit expected):")
puts(obj.value)

puts("Third call (hit expected):")
puts(obj.value)

puts("Testing user method:")
puts("First call (miss expected):")
puts(obj.double())

puts("Second call (hit expected):")
puts(obj.double())

puts("Third call (hit expected):")
puts(obj.double())
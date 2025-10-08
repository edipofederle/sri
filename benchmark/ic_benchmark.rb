class BenchmarkClass
  attr_accessor :x, :y, :z
  
  def initialize(a, b, c)
    @x = a
    @y = b
    @z = c
  end
  
  def compute
    (@x + @y) * @z
  end
  
  def update(new_x, new_y, new_z)
    @x = new_x
    @y = new_y
    @z = new_z
  end
end

# Create multiple objects
obj1 = BenchmarkClass.new(1, 2, 3)
obj2 = BenchmarkClass.new(4, 5, 6)
obj3 = BenchmarkClass.new(7, 8, 9)

puts("Starting benchmark...")

# Heavy method call workload
i = 0
while i < 1000
  # Getter calls (should benefit from IC)
  val1 = obj1.x
  val2 = obj1.y
  val3 = obj1.z
  
  val4 = obj2.x
  val5 = obj2.y
  val6 = obj2.z
  
  val7 = obj3.x
  val8 = obj3.y
  val9 = obj3.z
  
  # Method calls (should benefit from IC)
  result1 = obj1.compute()
  result2 = obj2.compute()
  result3 = obj3.compute()
  
  # Setter calls (should benefit from IC)
  obj1.x = result1
  obj2.y = result2
  obj3.z = result3
  
  i = i + 1
end

puts("Benchmark complete!")
puts("Final values:")
puts(obj1.x)
puts(obj2.y) 
puts(obj3.z)
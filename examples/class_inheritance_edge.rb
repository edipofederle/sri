# Test class method calls and instance variable edge cases
# expected-output: 0
# expected-output: 5
# expected-output: 10
# expected-output: Point: 3, 4

class Point
  def initialize(x, y)
    @x = x
    @y = y
  end
  
  def get_x
    @x
  end
  
  def set_x(new_x)
    @x = new_x
  end
  
  def distance_from_origin
    @x + @y  # simplified distance
  end
  
  def to_string
    "Point: " + @x + ", " + @y
  end
end

p = Point.new(0, 0)
puts(p.get_x)

p.set_x(5)
puts(p.get_x)

p2 = Point.new(3, 7)
puts(p2.distance_from_origin)

p3 = Point.new(3, 4)
puts(p3.to_string)
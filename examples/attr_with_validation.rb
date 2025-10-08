# expected-output: Testing Rectangle class:
# expected-output: Rectangle width: 5
# expected-output: Rectangle height: 3
# expected-output: Area: 15
# expected-output: Perimeter: 16
# expected-output: Modified rectangle width: 8
# expected-output: Modified rectangle height: 6
# expected-output: New area: 48
# expected-output: Scaled rectangle width: 16
# expected-output: Scaled rectangle height: 12
# expected-output: Testing Counter class:
# expected-output: Initial counter value: 10
# expected-output: After increment: 11
# expected-output: After decrement: 10
# expected-output: Set directly to 100: 100
# expected-output: After reset: 0

class Rectangle
  attr_accessor :width, :height

  def initialize(width, height)
    @width = width
    @height = height
  end

  def area
    @width * @height
  end

  def perimeter
    (@width + @height) * 2
  end

  def scale(factor)
    @width = @width * factor
    @height = @height * factor
  end
end

class Counter
  attr_accessor :value

  def initialize(start_value)
    @value = start_value
  end

  def increment
    @value = @value + 1
  end

  def decrement
    @value = @value - 1
  end

  def reset
    @value = 0
  end
end

# Test Rectangle class
puts("Testing Rectangle class:")
rect = Rectangle.new(5, 3)
puts("Rectangle width: " + rect.width.to_s)
puts("Rectangle height: " + rect.height.to_s)
puts("Area: " + rect.area.to_s)
puts("Perimeter: " + rect.perimeter.to_s)

# Modify dimensions using attr_accessor setters
rect.width = 8
rect.height = 6
puts("Modified rectangle width: " + rect.width.to_s)
puts("Modified rectangle height: " + rect.height.to_s)
puts("New area: " + rect.area.to_s)

# Scale the rectangle
rect.scale(2)
puts("Scaled rectangle width: " + rect.width.to_s)
puts("Scaled rectangle height: " + rect.height.to_s)

# Test Counter class
puts("Testing Counter class:")
counter = Counter.new(10)
puts("Initial counter value: " + counter.value.to_s)

counter.increment
puts("After increment: " + counter.value.to_s)

counter.decrement
puts("After decrement: " + counter.value.to_s)

# Use attr_accessor setter directly
counter.value = 100
puts("Set directly to 100: " + counter.value.to_s)

counter.reset
puts("After reset: " + counter.value.to_s)

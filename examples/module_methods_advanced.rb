# expected-output: Circle area (radius 5):
# expected-output: 75
# expected-output: Circle circumference (radius 5):
# expected-output: 30
# expected-output: Rectangle area (4x6):
# expected-output: 24
# expected-output: Rectangle perimeter (4x6):
# expected-output: 20
module Geometry
  module Circle
    def self.area(radius)
      radius * radius * 3
    end

    def self.circumference(radius)
      2 * radius * 3
    end
  end

  module Rectangle
    def self.area(width, height)
      width * height
    end

    def self.perimeter(width, height)
      2 * (width + height)
    end
  end
end

puts("Circle area (radius 5):")
puts(Geometry::Circle.area(5))

puts("Circle circumference (radius 5):")
puts(Geometry::Circle.circumference(5))

puts("Rectangle area (4x6):")
puts(Geometry::Rectangle.area(4, 6))

puts("Rectangle perimeter (4x6):")
puts(Geometry::Rectangle.perimeter(4, 6))

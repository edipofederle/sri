# expected-output: Created sedan:
# expected-output: Toyota sedan with 4 doors
# expected-output: Created truck:
# expected-output: Ford truck with 2 doors

module CarFactory
  def self.create_sedan(brand)
    {
      "type" => "sedan",
      "brand" => brand,
      "wheels" => 4,
      "doors" => 4
    }
  end

  def self.create_truck(brand)
    {
      "type" => "truck",
      "brand" => brand,
      "wheels" => 4,
      "doors" => 2
    }
  end

  def self.get_info(car)
    car["brand"] + " " + car["type"] + " with " + car["doors"] + " doors"
  end
end

sedan = CarFactory.create_sedan("Toyota")
truck = CarFactory.create_truck("Ford")

puts("Created sedan:")
puts(CarFactory.get_info(sedan))

puts("Created truck:")
puts(CarFactory.get_info(truck))

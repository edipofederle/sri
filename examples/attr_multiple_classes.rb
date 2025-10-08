# expected-output: 2020 Toyota Camry
# expected-output: Make: Toyota
# expected-output: Year: 2020
# expected-output: VIN: ABC123456
# expected-output: Updated car:
# expected-output: 2020 Honda Civic
# expected-output: Driver: Mike
# expected-output: Age: 25
# expected-output: License: DL987654
# expected-output: Updated driver: Michael
# expected-output: Michael is driving the Honda Civic

class Car
  attr_accessor :make, :model
  attr_reader :year, :vin
  attr_writer :mileage

  def initialize(make, model, year, vin)
    @make = make
    @model = model
    @year = year
    @vin = vin
    @mileage = 0
  end

  def description
    puts(@year.to_s + " " + @make + " " + @model)
  end
end

class Driver
  attr_accessor :name, :license_number
  attr_reader :age

  def initialize(name, age, license_number)
    @name = name
    @age = age
    @license_number = license_number
  end

  def can_drive_car(car)
    puts(@name + " is driving the " + car.make + " " + car.model)
  end
end

# Create objects
car = Car.new("Toyota", "Camry", 2020, "ABC123456")
driver = Driver.new("Mike", 25, "DL987654")

# Test car attributes
car.description
puts("Make: " + car.make)
puts("Year: " + car.year.to_s)
puts("VIN: " + car.vin)

# Modify car attributes
car.make = "Honda"
car.model = "Civic"
car.mileage = 15000  # attr_writer only

puts("Updated car:")
car.description

# Test driver attributes
puts("Driver: " + driver.name)
puts("Age: " + driver.age.to_s)
puts("License: " + driver.license_number)

# Modify driver attributes
driver.name = "Michael"
driver.license_number = "DL555777"

puts("Updated driver: " + driver.name)

# Test interaction
driver.can_drive_car(car)

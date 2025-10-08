# expected-output: Name: Alice
# expected-output: Age: 25
# expected-output: Updated name: Bob
# expected-output: Updated age: 30
# expected-output: Hello, I'm Bob and I'm 30 years old
class Person
  attr_accessor :name, :age

  def initialize(name, age)
    @name = name
    @age = age
  end

  def greet
    puts("Hello, I'm " + @name + " and I'm " + @age.to_s + " years old")
  end
end

# Create a person
person = Person.new("Alice", 25)

# Test getter methods
puts("Name: " + person.name)
puts("Age: " + person.age.to_s)

# Test setter methods
person.name = "Bob"
person.age = 30

# Verify changes
puts("Updated name: " + person.name)
puts("Updated age: " + person.age.to_s)

# Call instance method
person.greet

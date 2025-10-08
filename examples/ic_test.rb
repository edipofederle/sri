class Person
  attr_accessor :name, :age
  
  def initialize(n, a)
    @name = n
    @age = a
  end
  
  def greet
    puts("Hello, I'm #{@name} and I'm #{@age} years old")
  end
end

# Create multiple instances to test IC transitions
person1 = Person.new("Alice", 25)
person2 = Person.new("Bob", 30)
person3 = Person.new("Charlie", 35)

# Test monomorphic IC (same class, same method)
puts("Testing attr_accessor getter:")
puts(person1.name)
puts(person2.name)
puts(person3.name)

puts("\nTesting attr_accessor setter:")
person1.name = "Alice Updated"
person2.name = "Bob Updated"
person3.name = "Charlie Updated"

puts("After updates:")
puts(person1.name)
puts(person2.name)
puts(person3.name)

puts("\nTesting user-defined method:")
person1.greet()
person2.greet()
person3.greet()
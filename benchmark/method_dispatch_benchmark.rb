class Person
  attr_accessor :name, :age

  def initialize(n, a)
    @name = n
    @age = a
  end

  def greeting
    "Hello, I'm " + @name
  end
end

person1 = Person.new("Alice", 25)
person2 = Person.new("Bob", 30)

puts("Starting method call benchmark...")

i = 0
while i < 50
  n1 = person1.name
  a1 = person1.age
  n2 = person2.name
  a2 = person2.age

  g1 = person1.greeting()
  g2 = person2.greeting()

  person1.name = n1
  person1.age = a1
  person2.name = n2
  person2.age = a2

  i = i + 1
end

puts("Method call benchmark complete!")
puts(person1.name)
puts(person2.name)

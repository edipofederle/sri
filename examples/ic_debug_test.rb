class Dog
  attr_accessor :name
  
  def initialize(n)
    @name = n
  end
  
  def bark
    puts("#{@name} says woof!")
  end
end

class Cat
  attr_accessor :name
  
  def initialize(n)
    @name = n
  end
  
  def bark
    puts("#{@name} says meow!")
  end
end

puts("=== IC Debug Test ===")
puts("Creating instances...")

dog = Dog.new("Rex")
cat = Cat.new("Whiskers")

puts("\n--- First calls (IC misses expected) ---")
puts(dog.name)     # Miss: empty -> monomorphic
puts(dog.bark())   # Miss: empty -> monomorphic

puts("\n--- Second calls (IC hits expected) ---")
puts(dog.name)     # Hit: monomorphic
puts(dog.bark())   # Hit: monomorphic

puts("\n--- Different class calls (polymorphic IC) ---")
puts(cat.name)     # Miss: monomorphic -> polymorphic  
puts(cat.bark())   # Miss: monomorphic -> polymorphic

puts("\n--- Mixed calls (should hit polymorphic cache) ---")
puts(dog.name)     # Hit: polymorphic
puts(cat.name)     # Hit: polymorphic
puts(dog.bark())   # Hit: polymorphic
puts(cat.bark())   # Hit: polymorphic

puts("\n=== IC Debug Test Complete ===")
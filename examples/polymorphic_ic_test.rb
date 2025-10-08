class Dog
  attr_accessor :name
  
  def initialize(n)
    @name = n
  end
  
  def speak
    puts("#{@name} says woof!")
  end
end

class Cat
  attr_accessor :name
  
  def initialize(n)
    @name = n  
  end
  
  def speak
    puts("#{@name} says meow!")
  end
end

class Bird
  attr_accessor :name
  
  def initialize(n)
    @name = n
  end
  
  def speak
    puts("#{@name} says chirp!")
  end
end

# Create animals of different classes
dog = Dog.new("Rex")
cat = Cat.new("Whiskers") 
bird = Bird.new("Tweety")

# Test polymorphic IC: same method name across different classes
puts("Testing polymorphic IC with attr_accessor:")
puts(dog.name)
puts(cat.name)  
puts(bird.name)

puts("\nTesting polymorphic IC with user methods:")
dog.speak()
cat.speak()
bird.speak()

puts("\nTesting IC hit rate with repeated calls:")
dog.speak()
dog.speak()
cat.speak()
cat.speak()
bird.speak()
bird.speak()
# Basic class definition with constructor
# expected-output: Creating a new person
# expected-output: Woof! A new dog is born
class Person
  def initialize
    puts("Creating a new person")
  end
end

class Dog
  def initialize
    puts("Woof! A new dog is born")
  end
end

Person.new
Dog.new
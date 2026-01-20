# expected-output: 10
# expected-output: 15
# expected-output: 15
# expected-output: 16
# expected-output: Hello!!!

# Test 1: Add multiple methods
class Integer
  def double
    self * 2
  end

  def triple
    self * 3
  end
end

puts(5.double())
puts(5.triple())

# # Test 2: Methods with parameters
class Integer
  def add(n)
    self + n
  end
end

puts(10.add(5))

# Test 3: Reopen the class again
class Integer
  def square
    self * self
  end
end

puts(4.square())

# # Test 4: Open String class
class String
  def yell
    self + "!!!"
  end
end

puts("Hello".yell())

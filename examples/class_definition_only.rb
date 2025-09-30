# Test class definition without instantiation
# expected-output: defined

class Calculator
  def add(a, b)
    a + b
  end
  
  def multiply(x, y)
    x * y
  end
end

puts("defined")
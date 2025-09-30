# Test: Simple stateless class with method calls
# expected-output: Hello from SimpleClass
# expected-output: 42
# expected-output: 100
# expected-output: 31

class SimpleClass
  def greet
    puts("Hello from SimpleClass")
  end

  def get_number
    42
  end

  def calculate
    50 + 50
  end

  def foo(a)
    10 + 20 + a
  end
end

obj = SimpleClass.new
obj.greet
puts(obj.get_number)
puts(obj.calculate)
puts(obj.foo(1))

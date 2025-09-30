# Test nested method calls and complex expressions
# expected-output: 8
# expected-output: Result: 15

def add(a, b)
  a + b
end

def multiply(a, b)
  a * b
end

def complex_calc(x)
  multiply(add(x, 2), 2)
end

puts(complex_calc(2))  # Should be (2+2)*2 = 8

# Mix with string concatenation
def format_result(num)
  "Result: " + num
end

puts(format_result(15))
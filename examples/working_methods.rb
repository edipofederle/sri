# Test: Method calls and arithmetic (no classes)
# expected-output: 42
# expected-output: 100
# expected-output: 25
# expected-output: 8
# expected-output: 15

def get_answer
  42
end

def square(n)
  n * n
end

def half(n)
  n / 2
end

def add(a, b)
  a + b
end

puts(get_answer)
puts(square(10))
puts(half(50))
puts(add(3, 5))
puts(add(square(3), half(12)))
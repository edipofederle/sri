# expected-output: 15
# expected-output: 5
# expected-output:  50
# expected-output: 2
# expected-output: 125

def add(a, b)
  a + b
end

def subtract(a, b)
  a - b
end

def multiply(a, b)
  a * b
end

def divide(a, b)
  a / b
end

def power(base, exp)
  if exp == 0
    1
  else
    base * power(base, exp - 1)
  end
end

x = 10
y = 5

puts(add(x, y))
puts(subtract(x, y))
puts(multiply(x, y))
puts(divide(x, y))
puts(power(y, 3))

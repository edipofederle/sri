# expected-output: 42
# expected-output: 100
# expected-output: 36
# expected-output: 21
# expected-output: 256

def double(n)
  n * 2
end

def square(n)
  n * n
end

def half(n)
  n / 2
end

def add_one(n)
  n + 1
end

base = 20
result1 = add_one(double(base) + 1)
puts(result1)

result2 = square(double(5))
puts(result2)

result3 = square(double(3))
puts(result3)

result4 = half(result1)
puts(result4)

power_base = 4
result5 = square(square(power_base))
puts(result5)

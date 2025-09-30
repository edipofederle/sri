# Test method parameters and return values edge cases
# expected-output: 0
# expected-output: 5
# expected-output: text
# expected-output: 42

def no_params
  0
end

def one_param(x)
  x
end

def string_param(s)
  s
end

def mixed_operations(a, b, c)
  (a + b) * c
end

puts(no_params)
puts(one_param(5))
puts(string_param("text"))
puts(mixed_operations(2, 5, 6))  # (2+5)*6 = 42
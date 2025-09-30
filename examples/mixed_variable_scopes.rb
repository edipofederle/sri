# Test variable scoping with methods and classes
# expected-output: 5
# expected-output: 3
# expected-output: 9

x = 5
puts(x)

def test_method(y)
  x = 3  # local variable in method
  puts(x)
  y + 4
end

result = test_method(x)
puts(result)
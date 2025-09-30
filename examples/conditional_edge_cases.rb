# Test edge cases in conditional logic
# expected-output: zero
# expected-output: false condition
# expected-output: nested true
# expected-output: complex true

# Test with zero
x = 0
if x == 0
  puts("zero")
else
  puts("not zero")
end

# Test nested conditionals with variables
a = 1
b = 2
if a > b
  puts("true condition")
else
  puts("false condition")
end

# Test deeply nested conditionals
if a < b
  if b > 1
    puts("nested true")
  else
    puts("nested false")
  end
else
  puts("outer false")
end

# Test complex conditional expressions
if (a + b) > 2
  puts("complex true")
else
  puts("complex false")
end
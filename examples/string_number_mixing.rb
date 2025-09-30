# Test mixing strings and numbers in various ways
# expected-output: Count: 5
# expected-output: Total: 7
# expected-output: Result: 3

def format_count(n)
  "Count: " + n
end

def add_with_message(a, b)
  result = a + b
  "Total: " + result
end

def subtract_and_format(x, y)
  diff = x - y
  "Result: " + diff
end

puts(format_count(5))
puts(add_with_message(3, 4))
puts(subtract_and_format(8, 5))
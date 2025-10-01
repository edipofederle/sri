# Test case/when statements
# expected-output: small
# expected-output: medium
# expected-output: large
# expected-output: unknown

def classify_number(n)
  case n
  when 1, 2, 3
    "small"
  when 4, 5, 6, 7, 8, 9
    "medium"
  when 10
    "large"
  else
    "unknown"
  end
end

puts(classify_number(2))
puts(classify_number(5))
puts(classify_number(10))
puts(classify_number(15))
# Test: Control flow with if/else statements and comparisons

# expected-output: positive
# expected-output: zero
# expected-output: negative
# expected-output: even
# expected-output: odd
# expected-output: large

def classify_number(n)
  if n > 0
    "positive"
  else
    if n == 0
      "zero"
    else
      "negative"
    end
  end
end

def even_or_odd(n)
  remainder = n - (n / 2) * 2
  if remainder == 0
    "even"
  else
    "odd"
  end
end

def size_category(n)
  if n > 100
    "large"
  else
    if n > 50
      "medium"
    else
      "small"
    end
  end
end

test1 = 5
test2 = 0
test3 = -3
test4 = 8
test5 = 7
test6 = 150

puts(classify_number(test1))
puts(classify_number(test2))
puts(classify_number(test3))
puts(even_or_odd(test4))
puts(even_or_odd(test5))
puts(size_category(test6))

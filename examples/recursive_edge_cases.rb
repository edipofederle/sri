# Test recursive functions with edge cases
# expected-output: 1
# expected-output: 1
# expected-output: 1
# expected-output: 6

def countdown(n)
  if n <= 0
    1
  else
    countdown(n - 1)
  end
end

def fibonacci(n)
  if n <= 1
    n
  else
    fibonacci(n - 1) + fibonacci(n - 2)
  end
end

def sum_to_n(n)
  if n <= 1
    n
  else
    n + sum_to_n(n - 1)
  end
end

puts(countdown(3))
puts(fibonacci(1))
puts(fibonacci(2))
puts(sum_to_n(3))
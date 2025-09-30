# Test: Fibonacci sequence with recursive methods and variables
# Expected output:
# 1
# 1
# 2
# 3
# 5
# 8
# 13

def fibonacci(n)
  if n <= 1
    n
  else
    fibonacci(n - 1) + fibonacci(n - 2)
  end
end

def print_fibonacci_sequence(count)
  i = 1
  while i <= count
    result = fibonacci(i)
    puts(result)
    i = i + 1
  end
end

sequence_length = 7
print_fibonacci_sequence(sequence_length)
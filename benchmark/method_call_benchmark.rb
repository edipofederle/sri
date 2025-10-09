# Method call benchmark
puts("Starting method call benchmark...")

# Simple method definition
def fibonacci(n)
  if n <= 1
    n
  else
    fibonacci(n - 1) + fibonacci(n - 2)
  end
end

def factorial(n)
  if n <= 1
    1
  else
    n * factorial(n - 1)
  end
end

# Method call intensive test
fib_results = []
fact_results = []

i = 1
while i <= 10
  fib_results[i - 1] = fibonacci(i)
  fact_results[i - 1] = factorial(i)
  i = i + 1
end

puts("Fibonacci results:")
j = 0
while j < fib_results.length
  puts(fib_results[j].to_s)
  j = j + 1
end

puts("Factorial results:")
k = 0
while k < fact_results.length
  puts(fact_results[k].to_s)
  k = k + 1
end

puts("Method call benchmark complete!")
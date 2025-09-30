# expected-output: 55
# expected-output: 120
# expected-output: 15
# expected-output: 6
#
# This example tests recursive method calls that previously caused
# JVM bytecode verification errors due to specialized method call
# optimization attempting to call non-existent method variants.
#
# The fix ensures all recursive calls use Object-based signatures
# while maintaining performance for built-in method optimizations.

# Fibonacci sequence - tests recursive calls with arithmetic
def fibonacci(n)
  if n <= 1
    n
  else
    fibonacci(n - 1) + fibonacci(n - 2)
  end
end

# Factorial - tests recursive calls with multiplication  
def factorial(n)
  if n <= 1
    1
  else
    n * factorial(n - 1)
  end
end

# Sum of numbers 1 to n - tests recursive calls with addition
def sum_to_n(n)
  if n <= 0
    0
  else
    n + sum_to_n(n - 1)
  end
end

# Greatest Common Divisor - tests recursive calls with modulo
def gcd(a, b)
  if b == 0
    a
  else
    gcd(b, a % b)
  end
end

# Test all recursive functions
puts(fibonacci(10))
puts(factorial(5))  
puts(sum_to_n(5))
puts(gcd(48, 18))
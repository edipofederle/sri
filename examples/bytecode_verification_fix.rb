# expected-output: 120
# expected-output: 8
#
# Bytecode Verification Fix Test
# =============================
#
# This example demonstrates the fix for JVM bytecode verification errors
# that occurred when recursive method calls attempted to use specialized
# method variants that didn't exist.
#
# Issue: The specialized method call optimization tried to call 'factorial_int'
# instead of 'factorial', causing VerifyError with inconsistent stackmap frames.
#
# Fix: Disabled specialized method call generation while preserving 
# monomorphic call site optimization for built-in methods.

def factorial(n)
  if n <= 1
    1
  else
    n * factorial(n - 1)
  end
end

def power(base, exp)
  if exp <= 0
    1
  else
    base * power(base, exp - 1)
  end
end

# These should work with both file and inline execution
puts(factorial(5))
puts(power(2, 3))
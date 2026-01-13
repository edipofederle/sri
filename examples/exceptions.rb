# expected-output: Caught: LocalJumpError - no block given (yield)
# expected-output: Caught: ArgumentError - wrong number of arguments (given 2, expected 1)
# expected-output: Caught: NoMethodError - undefined method `nonexistent_method` for class java.lang.Integer
# expected-output: Caught: NameError - undefined local variable or method `undefined_variable`
# expected-output: Caught: TypeError - no implicit conversion of class sri.ruby_string.RubyString into Integer
# expected-output: Inner rescue: Inner error
# expected-output: Outer rescue: Outer error
# expected-output: === Ensure Test ===
# expected-output: In try block
# expected-output: In rescue: Error!
# expected-output: In ensure (always executes)

# Test LocalJumpError
begin
  def test_yield
    yield
  end

  test_yield  # Should raise LocalJumpError: no block given
rescue => e
  puts("Caught: #{e.class} - #{e.message}")
end

# Test ArgumentError
begin
  Integer.sqrt(625, 10)  # Should raise ArgumentError: wrong number of arguments
rescue => e
  puts("Caught: #{e.class} - #{e.message}")
end

# Test NoMethodError
begin
  x = 5
  x.nonexistent_method  # Should raise NoMethodError
rescue => e
  puts("Caught: #{e.class} - #{e.message}")
end

# Test NameError
begin
  puts(undefined_variable)  # Should raise NameError
rescue => e
  puts("Caught: #{e.class} - #{e.message}")
end

# Test TypeError
begin
  arr = [1, 2, 3]
  arr["not_an_integer"]  # Should raise TypeError
rescue => e
  puts("Caught: #{e.class} - #{e.message}")
end

# Test nested exception handling
begin
  begin
    raise("Inner error")
  rescue => e
    puts("Inner rescue: #{e.message}")
    raise("Outer error")
  end
rescue => e
  puts("Outer rescue: #{e.message}")
end

# Test ensure clause
puts("=== Ensure Test ===")
begin
  puts("In try block")
  raise("Error!")
rescue => e
  puts("In rescue: #{e.message}")
ensure
  puts("In ensure (always executes)")
end

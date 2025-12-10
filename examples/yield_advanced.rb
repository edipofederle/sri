# expected-output: === Advanced Yield Examples ===
# expected-output: 
# expected-output: Yield with conditional execution:
# expected-output: Processing even: 2
# expected-output: Skipping odd: 3
# expected-output: Processing even: 4
# expected-output: Skipping odd: 5
# expected-output: Processing even: 6
# expected-output: 
# expected-output: Nested yield calls:
# expected-output: Outer: 1, Inner: a
# expected-output: Outer: 1, Inner: b
# expected-output: Outer: 2, Inner: a
# expected-output: Outer: 2, Inner: b
# expected-output: 
# expected-output: Yield with method chaining pattern:
# expected-output: Step 1: 5
# expected-output: Step 2: 10
# expected-output: Step 3: 20
# expected-output: Final result: 20
# expected-output: 
# expected-output: Fibonacci generator using yield:
# expected-output: Fibonacci sequence (first 8 numbers):
# expected-output: 0 1 1 2 3 5 8 13

puts("=== Advanced Yield Examples ===")
puts("")

# Conditional yield
def process_if_even(arr)
  i = 0
  while i < arr.length
    if arr[i] % 2 == 0
      puts("Processing even: " + arr[i].to_s)
      yield(arr[i])
    else
      puts("Skipping odd: " + arr[i].to_s)
    end
    i = i + 1
  end
end

puts("Yield with conditional execution:")
numbers = [2, 3, 4, 5, 6]
process_if_even(numbers) { |x| x * x }

puts("")

# Nested yields
def outer_iterator(nums)
  i = 0
  while i < nums.length
    yield(nums[i])
    i = i + 1
  end
end

def inner_iterator(letters)
  i = 0
  while i < letters.length
    yield(letters[i])
    i = i + 1
  end
end

puts("Nested yield calls:")
outer_nums = [1, 2]
inner_letters = ["a", "b"]

outer_iterator(outer_nums) do |num|
  inner_iterator(inner_letters) do |letter|
    puts("Outer: " + num.to_s + ", Inner: " + letter)
  end
end

puts("")

# Method chaining pattern with yield
def process_step(value, step_name)
  puts("Step " + step_name + ": " + value.to_s)
  return yield(value)
end

puts("Yield with method chaining pattern:")
initial_value = 5

final = process_step(initial_value, "1") do |x|
  next_val = x * 2
  process_step(next_val, "2") do |y|
    result = y * 2
    process_step(result, "3") { |z| z }
  end
end

puts("Final result: " + final.to_s)

puts("")

# Fibonacci generator
def fibonacci_generator(count)
  a = 0
  b = 1
  i = 0
  
  while i < count
    if i == 0
      yield(a)
    else
      if i == 1
        yield(b)
      else
        c = a + b
        yield(c)
        a = b
        b = c
      end
    end
    i = i + 1
  end
end

puts("Fibonacci generator using yield:")
puts("Fibonacci sequence (first 8 numbers):")
fibonacci_generator(8) { |fib| print(fib.to_s + " ") }
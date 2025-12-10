# expected-output: === Custom Iterator Examples ===
# expected-output: 
# expected-output: Custom each implementation:
# expected-output: Processing: apple
# expected-output: Processing: banana
# expected-output: Processing: cherry
# expected-output: 
# expected-output: Custom each_with_index:
# expected-output: 0: red
# expected-output: 1: green
# expected-output: 2: blue
# expected-output: 
# expected-output: Custom times implementation:
# expected-output: Iteration 0
# expected-output: Iteration 1
# expected-output: Iteration 2
# expected-output: Iteration 3
# expected-output: Iteration 4
# expected-output: 
# expected-output: Custom map implementation:
# expected-output: Original: [1 2 3 4]
# expected-output: Doubled: [2 4 6 8]
# expected-output: 
# expected-output: Custom filter implementation:
# expected-output: Numbers: [1 2 3 4 5 6 7 8]
# expected-output: Even numbers: [2 4 6 8]

puts("=== Custom Iterator Examples ===")
puts("")

# Custom each implementation
def my_each(arr)
  i = 0
  while i < arr.length
    yield(arr[i])
    i = i + 1
  end
end

puts("Custom each implementation:")
fruits = ["apple", "banana", "cherry"]
my_each(fruits) { |fruit| puts("Processing: " + fruit) }

puts("")

# Custom each_with_index
def my_each_with_index(arr)
  i = 0
  while i < arr.length
    yield(i, arr[i])
    i = i + 1
  end
end

puts("Custom each_with_index:")
colors = ["red", "green", "blue"]
my_each_with_index(colors) { |index, color| puts(index.to_s + ": " + color) }

puts("")

# Custom times implementation
def my_times(n)
  i = 0
  while i < n
    yield(i)
    i = i + 1
  end
end

puts("Custom times implementation:")
my_times(5) { |i| puts("Iteration " + i.to_s) }

puts("")

# Custom map implementation
def my_map(arr)
  result = []
  i = 0
  while i < arr.length
    mapped_value = yield(arr[i])
    result[i] = mapped_value
    i = i + 1
  end
  return result
end

puts("Custom map implementation:")
numbers = [1, 2, 3, 4]
doubled = my_map(numbers) { |x| x * 2 }
puts("Original: " + numbers.to_s)
puts("Doubled: " + doubled.to_s)

puts("")

# Custom filter implementation
def my_filter(arr)
  result = []
  result_index = 0
  i = 0
  while i < arr.length
    if yield(arr[i])
      result[result_index] = arr[i]
      result_index = result_index + 1
    end
    i = i + 1
  end
  return result
end

puts("Custom filter implementation:")
all_numbers = [1, 2, 3, 4, 5, 6, 7, 8]
even_numbers = my_filter(all_numbers) { |x| x % 2 == 0 }
puts("Numbers: " + all_numbers.to_s)
puts("Even numbers: " + even_numbers.to_s)
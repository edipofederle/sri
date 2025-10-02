# Test for loops
arr = [1, 2, 3, 4, 5]

puts("For loop test:")
for item in arr
  puts(item)
end

puts("For loop with break:")
for item in arr
  if item == 3
    break
  end
  puts(item)
end

puts("For loop with next:")
for item in arr
  if item == 3
    next
  end
  puts(item)
end
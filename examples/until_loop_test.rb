# Test until loops
i = 0

puts("Until loop test:")
until i == 5
  puts(i)
  i = i + 1
end

puts("Until loop with break:")
i = 0
until i == 10
  if i == 3
    break
  end
  puts(i)
  i = i + 1
end

puts("Until loop with next:")
i = 0
until i == 5
  i = i + 1
  if i == 3
    next
  end
  puts(i)
end
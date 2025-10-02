# Test infinite loop with break
i = 0

puts("Loop do...end test:")
loop do
  puts(i)
  i = i + 1
  if i == 3
    break
  end
end

puts("Done!")
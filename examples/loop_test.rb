# expected-output: Testing loop do...end:
# expected-output: Iteration: 0
# expected-output: Iteration: 1
# expected-output: Iteration: 2
# expected-output: Iteration: 3
# expected-output: Iteration: 4
# expected-output: Loop finished with counter = 5
# expected-output:
# expected-output: Testing loop with next:
# expected-output: Odd number: 1
# expected-output: Odd number: 3
# expected-output: Odd number: 5
# expected-output: Odd number: 7
# expected-output: Odd number: 9
# expected-output: Odd number: 11
# expected-output: Done!


puts("Testing loop do...end:")

counter = 0

loop do
  puts("Iteration: " + counter.to_s)
  counter = counter + 1

  if counter >= 5
    break
  end
end

puts("Loop finished with counter = " + counter.to_s)

# Test with next
puts("\nTesting loop with next:")
i = 0

loop do
  i = i + 1

  if i % 2 == 0
    if i > 10
      break
    end
    next
  end

  puts("Odd number: " + i.to_s)

  if i > 10
    break
  end
end

puts("Done!")

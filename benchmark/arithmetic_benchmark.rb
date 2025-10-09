puts("Starting arithmetic benchmark...")

sum = 0
i = 0
while i < 1000
  sum = sum + i * 2 - 1
  i = i + 1
end

puts("Sum result: " + sum.to_s)

result = 0
j = 1
while j <= 100
  result = result + (1000 / j) + (1000 % j)
  j = j + 1
end

puts("Division/modulo result: " + result.to_s)
puts("Arithmetic benchmark complete!")

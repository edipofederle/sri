puts("Starting control flow benchmark...")

result = 0
i = 0
while i < 50
  j = 0
  while j < 20
    if (i + j) % 2 == 0
      result = result + i
    else
      result = result + j
    end
    j = j + 1
  end
  i = i + 1
end

puts("Nested loop result: " + result.to_s)

score = 0
test_values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

k = 0
while k < test_values.length
  val = test_values[k]

  if val < 3
    score = score + 1
  else
    if val < 6
      score = score + 2
    else
      if val < 9
        score = score + 3
      else
        score = score + 5
      end
    end
  end

  k = k + 1
end

puts("Conditional score: " + score.to_s)

sum = 0
m = 1
while m <= 100
  if m % 3 == 0
    sum = sum + m
  end
  m = m + 1
end

puts("Divisible by 3 sum: " + sum.to_s)
puts("Control flow benchmark complete!")

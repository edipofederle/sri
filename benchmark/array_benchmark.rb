puts("Starting array benchmark...")

arr = [1, 2, 3, 4, 5]
sum = 0
i = 0
while i < 200
  val = arr[i % 5]
  sum = sum + val
  i = i + 1
end

puts("Array access sum: " + sum.to_s)

lengths = []
j = 0

while j < 50
  test_array = [j, j + 1, j + 2]
  lengths[j] = test_array.length
  j = j + 1
end

total_length = 0
k = 0
while k < lengths.length
  total_length = total_length + lengths[k]
  k = k + 1
end

puts("Total lengths: " + total_length.to_s)
puts("Array benchmark complete!")

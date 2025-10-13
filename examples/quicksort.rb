# expected-output: Original array:
# expected-output: [64 34 25 12 22 11 90 5 77 30]
# expected-output: Sorted array:
# expected-output: [5 11 12 22 25 30 34 64 77 90]

def quicksort(arr)
  if arr.length() <= 1
    return arr
  end

  pivot = arr[0]
  left = []
  right = []

  i = 1
  while i < arr.length()
    if arr[i] < pivot
      left.push(arr[i])
    else
      right.push(arr[i])
    end
    i = i + 1
  end

  sorted_left = quicksort(left)
  sorted_right = quicksort(right)
  pivot_arr = [pivot]

  result = sorted_left + pivot_arr + sorted_right
  return result
end

test_array = [64, 34, 25, 12, 22, 11, 90, 5, 77, 30]
puts("Original array:")
puts(test_array.to_s())

sorted = quicksort(test_array)
puts("Sorted array:")
puts(sorted.to_s())

# expected-output: 1
# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: 3
# expected-output: 9
# expected-output: 345

def bubble_sort(array)
  n = array.length
  i = 0

  while i < n
    j = 0
    while j < n - i - 1
      if array[j] > array[j + 1]
        temp = array[j]
        array[j] = array[j + 1]
        array[j + 1] = temp
      end
      j = j + 1
    end
    i = i + 1
  end

  array
end

puts(bubble_sort([9,2,1,3,3,1,345]))
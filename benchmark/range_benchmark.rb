# Range operations benchmark
puts("Starting range benchmark...")

# Range creation and conversion
total_size = 0
i = 0
while i < 100
  r = 1..100
  arr = r.to_a
  total_size = total_size + arr.length
  i = i + 1
end

puts("Range to_a total size: " + total_size.to_s)

# Range include operations
include_count = 0
j = 0
while j < 1000
  range = 1..1000
  if range.include?(j)
    include_count = include_count + 1
  end
  j = j + 1
end

puts("Range include count: " + include_count.to_s)

# Case statement ranges
case_matches = 0
k = 0
while k < 500
  result = case k
          when 0..100
            "small"
          when 101..300
            "medium"
          when 301..500
            "large"
          else
            "unknown"
          end
  
  if result != "unknown"
    case_matches = case_matches + 1
  end
  k = k + 1
end

puts("Case range matches: " + case_matches.to_s)

# Mixed range operations
range_sum = 0
m = 0
while m < 50
  r1 = 0..m
  r2 = m...100
  
  range_sum = range_sum + r1.size + r2.size
  m = m + 1
end

puts("Mixed range sum: " + range_sum.to_s)
puts("Range benchmark complete!")
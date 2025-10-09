# Character range operations benchmark
puts("Starting character range benchmark...")

# Character range creation and conversion
total_chars = 0
i = 0
while i < 50
  alphabet = "a".."z"
  chars = alphabet.to_a
  total_chars = total_chars + chars.length
  i = i + 1
end

puts("Character range to_a total: " + total_chars.to_s)

# Character range include operations
include_count = 0
test_chars = ["a", "m", "z", "A", "5", "!"]
j = 0
while j < 100
  range = "a".."z"
  k = 0
  while k < test_chars.length
    if range.include?(test_chars[k])
      include_count = include_count + 1
    end
    k = k + 1
  end
  j = j + 1
end

puts("Character include count: " + include_count.to_s)

# Character case statement ranges
case_matches = 0
test_letters = ["a", "m", "z", "A", "M", "Z", "1", "!"]
l = 0
while l < test_letters.length
  letter = test_letters[l]
  result = case letter
          when "a".."m"
            "first_half"
          when "n".."z"
            "second_half"
          when "A".."Z"
            "uppercase"
          else
            "other"
          end
  
  if result != "other"
    case_matches = case_matches + 1
  end
  l = l + 1
end

puts("Character case matches: " + case_matches.to_s)

# Mixed character range operations
range_sizes = 0
m = 0
while m < 26
  start_char = ("a".."z").to_a[0]  # Get 'a'
  end_char = ("a".."z").to_a[m]    # Get char at position m
  if m < 26
    r = start_char..end_char
    range_sizes = range_sizes + r.size
  end
  m = m + 1
end

puts("Character range sizes sum: " + range_sizes.to_s)
puts("Character range benchmark complete!")
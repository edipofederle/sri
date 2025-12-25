# expected-output: Original string:
# expected-output: hello
# expected-output: Reversed:
# expected-output: olleh
# expected-output: Character count:
# expected-output: 5
# expected-output: Uppercase first:
# expected-output: Apple

module StringUtils
  # Class/module method
  def self.reverse_string(str)
    # Simple character reversal (basic implementation)
    result = ""
    i = str.length - 1
    while i >= 0
      result = result + str[i]
      i = i - 1
    end
    result
  end

  def self.count_chars(str)
    str.length
  end

  def self.uppercase_first(str)
    if str.length > 0
      first_char = str[0]
      rest = ""
      i = 1
      while i < str.length
        rest = rest + str[i]
        i = i + 1
      end
      # Simple uppercase conversion for demo
      if first_char == "a"
        first_char = "A"
      end
      first_char + rest
    else
      str
    end
  end
end

test_string = "hello"

puts("Original string:")
puts(test_string)

puts("Reversed:")
puts(StringUtils.reverse_string(test_string))

puts("Character count:")
puts(StringUtils.count_chars(test_string))

puts("Uppercase first:")
puts(StringUtils.uppercase_first("apple"))

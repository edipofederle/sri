# Test method return value edge cases
# expected-output: 42
# expected-output: last line
# expected-output: 10

def explicit_return
  return 42
  99  # This should not be reached
end

def implicit_return
  "first line"
  "last line"  # This should be returned
end

def conditional_return(flag)
  if flag
    10
  else
    20
  end
end

puts(explicit_return)
puts(implicit_return)
puts(conditional_return(1))
# expected-output: About to raise an error
# expected-output: Error caught: Something went wrong
# expected-output: Continuing execution

begin
  puts("About to raise an error")
  raise("Something went wrong")
  puts("This line should not be executed")
rescue
  puts("Error caught: Something went wrong")
end

puts("Continuing execution")

# expected-output: Testing simple raise
# expected-output: Before raise
# expected-output: Caught: Simple error message
# expected-output: After rescue
puts("Testing simple raise")

begin
  puts("Before raise")
  raise("Simple error message")
  puts("Should not reach here")
rescue => e
  puts("Caught: #{e.message}")
end

puts("After rescue")
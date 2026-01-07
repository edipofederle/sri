# expected-output: Testing multiple rescue clauses
# expected-output: First rescue caught: First error
# expected-output: Second rescue caught: Second error
# expected-output: Final rescue caught: Third error

puts("Testing multiple rescue clauses")

# Test first rescue clause
begin
  raise("First error")
rescue => e1
  puts("First rescue caught: #{e1.message}")
end

# Test second rescue clause
begin
  raise("Second error")
rescue => e2
  puts("Second rescue caught: #{e2.message}")
end

# Test final rescue clause
begin
  raise("Third error")
rescue => e3
  puts("Final rescue caught: #{e3.message}")
end

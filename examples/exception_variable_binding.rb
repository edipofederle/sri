# expected-output: Testing exception variable binding
# expected-output: Caught exception: RuntimeError - Network connection failed
# expected-output: Error details processed

puts("Testing exception variable binding")

begin
  raise("Network connection failed")
rescue => e
  puts("Caught exception: #{e.class} - #{e.message}")
end

puts("Error details processed")

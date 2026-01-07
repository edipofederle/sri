# expected-output: Beginning operation
# expected-output: About to fail
# expected-output: Rescue: Operation failed
# expected-output: Ensure: Cleanup code executed
# expected-output: Operation complete

begin
  puts("Beginning operation")
  puts("About to fail")
  raise("Operation failed")
  puts("This should not execute")
rescue => e
  puts("Rescue: #{e.message}")
ensure
  puts("Ensure: Cleanup code executed")
end

puts("Operation complete")

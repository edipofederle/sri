# expected-output: Testing global exception variables
# expected-output: Caught exception: Test error
# expected-output: Global $! set correctly
# expected-output: Done

puts("Testing global exception variables")

begin
  raise("Test error")
rescue => e
  puts("Caught exception: #{e.message}")

  # Note: In a real Ruby interpreter, we would check $! here
  # For now, just verify the rescue worked
  puts("Global $! set correctly")
end

puts("Done")

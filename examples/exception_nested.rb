# expected-output: Testing nested exception handling
# expected-output: Outer begin block
# expected-output: Inner begin block
# expected-output: Inner rescue: Inner error occurred
# expected-output: After inner rescue
# expected-output: Outer rescue: Outer error occurred
# expected-output: All done

puts("Testing nested exception handling")

begin
  puts("Outer begin block")

  begin
    puts("Inner begin block")
    raise("Inner error occurred")
    puts("This should not print")
  rescue => inner_e
    puts("Inner rescue: #{inner_e.message}")
  end

  puts("After inner rescue")
  raise("Outer error occurred")
  puts("This should not print either")
rescue => outer_e
  puts("Outer rescue: #{outer_e.message}")
end

puts("All done")

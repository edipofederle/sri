# Test range printing directly and in arrays
puts("Direct range printing:")
r = 1..5
puts(r)

r2 = 10...20
puts(r2)

puts("\nRange in array:")
arr = [1...10]
puts(arr)

puts("\nMixed array:")
arr2 = [1..5, 10...15, :symbol, "string", 42]
puts(arr2)
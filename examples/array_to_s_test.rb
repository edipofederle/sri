# expected-output:  Array with range: [1...10]
# expected-output:  Mixed array: [1..5 "a".."e" symbol "string"]

arr = [1...10]
puts("Array with range: " + arr.to_s)

mixed_arr = [1..5, "a".."e", :symbol, "string"]
puts("Mixed array: " + mixed_arr.to_s)

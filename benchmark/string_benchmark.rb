puts("Starting string benchmark...")

result = ""
i = 0
while i < 100
  result = result + "test" + i.to_s + " "
  i = i + 1
end

puts("String length: " + result.length.to_s)

name = "Sri"
version = "1.0"
message = ""
j = 0
while j < 50
  message = "Hello from " + name + " version " + version + " iteration " + j.to_s
  j = j + 1
end

puts("Final message: " + message)
puts("String benchmark complete!")

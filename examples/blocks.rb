# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: Hello
# expected-output: world

[1, 2, 3].each do
  |x| puts(x)
end

["Hello", "world"].each { |x| puts(x) }

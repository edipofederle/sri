# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: Hello
# expected-output: world

[1, 2, 3].each { |x| puts(x) }

["Hello", "world"].each { |x| puts(x) }

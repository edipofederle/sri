# expected-output: 100
puts(Integer.max(100,20))

# expected-output: 25
puts(Integer.sqrt(625))

# expected-output: true
puts(42.positive?)

# expected-output: false
puts(42.negative?)

# expected-output: true
puts(0.zero?)

# expected-output: false
puts(42.zero?)

# expected-output: true
puts(42.even?)

# expected-output: false
puts(43.even?)

# expected-output: true
puts(42.real?)

# expected-output: true
puts(42.integer?)

# expected-output: 43
puts(42.inc)

# expected-output: 47
puts(42.incn(5))

# expected-output: 84
puts(42.double)
# expected-output: 2200
# expected-output: 40
# expected-output: 5

def foo(a)
a + 10
end

c = 20;

puts(foo(100) * c)
puts(foo(10) + c)
puts(foo(100) / c)

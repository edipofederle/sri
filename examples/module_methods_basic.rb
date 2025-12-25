# Basic module methods example
# expected-output: Testing module methods:
# expected-output: 8
# expected-output: 28
# expected-output: 36
module MathUtils
  def self.add(a, b)
    a + b
  end

  def self.multiply(x, y)
    x * y
  end

  def self.square(n)
    n * n
  end
end

puts("Testing module methods:")
puts(MathUtils.add(5, 3))
puts(MathUtils.multiply(4, 7))
puts(MathUtils.square(6))

# expected-output: 42
# expected-output: 1
# expected-output: 100
# expected-output: 20
# expected-output: 60

class Node
  attr_accessor :value, :left, :right, :height

  def initialize(val)
    @value = val
    @left = nil
    @right = nil
    @height = 1
  end
end

# Test creating a node and using accessors
node = Node.new(42)
puts(node.value)   # Should print 42
puts(node.height)  # Should print 1

# Test setters
node.value = 100
node.left = Node.new(20)
node.right = Node.new(60)

puts(node.value)     # Should print 100
puts(node.left.value)   # Should print 20
puts(node.right.value)  # Should print 60

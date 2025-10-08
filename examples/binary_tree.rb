# expected-output: Tree values in order:
# expected-output: 20
# expected-output: 30
# expected-output: 40
# expected-output: 50
# expected-output: 60
# expected-output: 70
# expected-output: 80
# expected-output: Root value: 50
# expected-output: Root height: 1
# expected-output: Modified root value: 55
# expected-output: Modified root height: 3

class TreeNode
  attr_accessor :value, :left, :right, :height

  def initialize(value)
    @value = value
    @left = nil
    @right = nil
    @height = 1
  end

  def insert(new_value)
    if new_value < @value
      if @left == nil
        @left = TreeNode.new(new_value)
      else
        @left.insert(new_value)
      end
    else
      if @right == nil
        @right = TreeNode.new(new_value)
      else
        @right.insert(new_value)
      end
    end
  end

  def in_order_traversal
    if @left != nil
      @left.in_order_traversal
    end
    puts(@value.to_s)
    if @right != nil
      @right.in_order_traversal
    end
  end
end

# Create root node
root = TreeNode.new(50)

# Insert values
root.insert(30)
root.insert(70)
root.insert(20)
root.insert(40)
root.insert(60)
root.insert(80)

puts("Tree values in order:")
root.in_order_traversal

# Test attr_accessor functionality
puts("Root value: " + root.value.to_s)
puts("Root height: " + root.height.to_s)

# Modify values using setters
root.value = 55
root.height = 3

puts("Modified root value: " + root.value.to_s)
puts("Modified root height: " + root.height.to_s)

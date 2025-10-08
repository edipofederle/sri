class Counter
  attr_accessor :value
  
  def initialize
    @value = 0
  end
  
  def increment
    @value = @value + 1
  end
end

counter = Counter.new()

puts("Testing same object, multiple calls:")
3.times do
  puts(counter.increment())
end
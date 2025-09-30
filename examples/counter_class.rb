# expected-output: 0
# expected-output: 1
# expected-output: 2
# expected-output: 3
# expected-output: 0

class Counter
  def initialize(start)
    @count = start
  end

  def increment
    @count = @count + 1
    @count
  end

  def get_value
    @count
  end

  def reset
    @count = 0
    @count
  end
end

counter = Counter.new(0)
puts(counter.get_value)
puts(counter.increment)
puts(counter.increment)
puts(counter.increment)
puts(counter.reset)

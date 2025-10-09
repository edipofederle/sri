puts("Starting class benchmark...")

class Calculator
  attr_accessor :value

  def initialize(initial)
    @value = initial
  end

  def add(n)
    @value = @value + n
  end

  def multiply(n)
    @value = @value * n
  end

  def get_value
    @value
  end
end

class Counter
  def initialize
    @count = 0
  end

  def increment
    @count = @count + 1
  end

  def get_count
    @count
  end
end

calculators = []
counters = []

i = 0
while i < 20
  calc = Calculator.new(i)
  counter = Counter.new()

  calc.add(5)
  calc.multiply(2)

  j = 0
  while j < 10
    counter.increment()
    j = j + 1
  end

  calculators[i] = calc
  counters[i] = counter
  i = i + 1
end

total_calc_value = 0
total_counter_value = 0

k = 0
while k < calculators.length
  total_calc_value = total_calc_value + calculators[k].get_value()
  total_counter_value = total_counter_value + counters[k].get_count()
  k = k + 1
end

puts("Total calculator value: " + total_calc_value.to_s)
puts("Total counter value: " + total_counter_value.to_s)
puts("Class benchmark complete!")

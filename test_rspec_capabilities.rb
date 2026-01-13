$test_results = []

def describe(description)
  puts("Describe: #{description}")
  yield
end

def it(description)
  begin
    yield
    $test_results << "PASS"
    puts("  It: #{description} (pass)")
  rescue => e
    $test_results << "FAIL"
    puts("  It: #{description} (fail) - #{e.message}")
  end
end

# --------------------------
# Expectations
# --------------------------

class Expectation
  def initialize(actual)
    @actual = actual
  end

  def to(matcher)
    if !matcher.matches?(@actual)
      raise(matcher.failure_message(@actual))
    end
  end
end

def expect(actual)
  Expectation.new(actual)
end

# --------------------------
# Matchers
# --------------------------

class EqMatcher
  def initialize(expected)
    @expected = expected
  end

  def matches?(actual)
    actual == @expected
  end

  def failure_message(actual)
    "expected #{actual.inspect} to equal #{@expected.inspect}"
  end
end

def eq(expected)
  EqMatcher.new(expected)
end

# --------------------------
# Example usage
# --------------------------

describe("Testing blocks") do
  it("passes when values are equal") do
    expect(10).to(eq(10))
  end

  it("fails when values are different") do
    expect(10).to(eq(10))
  end

  it("plus math") do
    x = 5 + 1
    expect(x).to(eq(10))
  end
end

puts("")

total = $test_results.length
pass_count = 0
i = 0

while i < total
  if $test_results[i] == "PASS"
    pass_count = pass_count + 1
  end
  i = i + 1
end

fail_count = total - pass_count

puts("#{pass_count} test pass, #{fail_count} test fail")

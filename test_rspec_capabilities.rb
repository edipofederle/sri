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

# describe("Testing blocks") do
#   it("passes when values are equal") do
#     expect(10).to(eq(10))
#   end

#   it("fails when values are different") do
#     expect(10).to(eq(10))
#   end

#   it("plus math") do
#     x = 5 + 1
#     expect(x).to(eq(10))
#   end
# end

class ShouldExpectation
  def initialize(actual)
    @actual = actual
  end

  def ==(expected)
    if @actual != expected
      raise("expected #{expected.inspect} but got #{@actual.inspect}")
    end
    true
  end
end

class Integer
  def should
    ShouldExpectation.new(self)
  end
end

class Float
  def should
    ShouldExpectation.new(self)
  end
end

# --------------------------
# raise_error matcher
# --------------------------

class RaiseErrorMatcher
  def initialize(expected_error)
    @expected_error = expected_error
    @has_expected = false
    # Store the class name for comparison
    if expected_error
      @has_expected = true
      @expected_name = expected_error.name
    end
  end

  def matches?(actual_error)
    if @has_expected
      # Compare class names as strings
      actual_error.class.name == @expected_name
    else
      # Any error matches
      true
    end
  end

  def failure_message_for_no_error
    "expected #{@expected_name} to be raised, but nothing was raised"
  end

  def failure_message_for_wrong_error(actual_error)
    "expected #{@expected_name} but got #{actual_error.class.name}"
  end
end

def raise_error(expected_error)
  RaiseErrorMatcher.new(expected_error)
end

# Proc class for lambda.should(matcher) support
class Proc
  def should(matcher)
    error_raised = nil
    begin
      self.call
    rescue => e
      error_raised = e
    end

    # If no error was raised, fail
    if error_raised
      # Check if matcher matches
      if matcher.matches?(error_raised)
        true
      else
        raise(matcher.failure_message_for_wrong_error(error_raised))
      end
    else
      raise(matcher.failure_message_for_no_error)
    end
  end
end

# Helper to test that code raises an error
# Usage: should_raise(NameError) { eval("_4_2") }
def should_raise(expected_error)
  actual_error = nil
  begin
    yield
  rescue => e
    actual_error = e
  end

  expected_name = expected_error.name

  if actual_error == nil
    raise("expected #{expected_name} to be raised, but nothing was raised")
  end

  actual_name = actual_error.class.name
  if actual_name != expected_name
    raise("expected #{expected_name} but got #{actual_name}")
  end
  true
end

# --------------------------
# Load test file from ARGV
# --------------------------

if ARGV.length > 0
  test_file = ARGV[0]
  load(test_file)
else
  puts("Usage: lein run test_rspec_capabilities.rb <test_file.rb>")
  puts("No test file provided.")
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

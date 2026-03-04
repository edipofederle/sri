$test_results = []
$before_blocks = []
$after_blocks = []

# ScratchPad for ruby/spec tests
class ScratchPad
  def self.record(value)
    $scratch_pad_value = value
  end

  def self.recorded
    $scratch_pad_value
  end

  def self.<<(item)
    $scratch_pad_value << item
  end

  def self.clear
    $scratch_pad_value = nil
  end
end

def describe(description)
  puts("Describe: #{description}")
  # Save current before/after blocks
  saved_before_blocks = $before_blocks
  saved_after_blocks = $after_blocks
  $before_blocks = []
  $after_blocks = []
  begin
    yield
  rescue => e
    puts("  ERROR in describe block: #{e.message}")
    $test_results << "ERROR"
  end
  # Restore previous before/after blocks
  $before_blocks = saved_before_blocks
  $after_blocks = saved_after_blocks
end

def before(scope, &block)
  # Store the block to run before each test
  $before_blocks << block
end

def after(scope, &block)
  # Store the block to run after each test
  $after_blocks << block
end

def it(description)
  test_result = nil
  test_error = nil
  begin
    # Run all before blocks first
    before_failed = false
    i = 0
    while i < $before_blocks.length
      begin
        $before_blocks[i].call
      rescue => be
        before_failed = true
        test_result = "ERROR"
        test_error = "error in before: #{be.message}"
      end
      i = i + 1
    end
    if !before_failed
      yield
      test_result = "PASS"
    end
  rescue => e
    test_result = "FAIL"
    test_error = e.message
  end

  # Run all after blocks (regardless of test result)
  i = 0
  while i < $after_blocks.length
    begin
      $after_blocks[i].call
    rescue => ae
      puts("  It: #{description} (error in after) - #{ae.message}")
    end
    i = i + 1
  end

  # Record and report the result
  $test_results << test_result
  if test_result == "PASS"
    puts("  It: #{description} (pass)")
  elsif test_result == "FAIL"
    puts("  It: #{description} (fail) - #{test_error}")
  elsif test_result == "ERROR"
    puts("  It: #{description} (#{test_error})")
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

# expect(value) or expect { block }
def expect(*args, &block)
  if block
    BlockExpectation.new(block)
  else
    Expectation.new(args[0])
  end
end

class BlockExpectation
  def initialize(block)
    @block = block
  end

  def to(matcher)
    error_raised = nil
    begin
      @block.call
    rescue => e
      error_raised = e
    end

    # For raise_error matcher
    if error_raised
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

# be_kind_of matcher
class BeKindOfMatcher
  def initialize(expected_class)
    @expected_class = expected_class
  end

  def matches?(actual)
    # Check if actual is a kind of the expected class
    # Use kind_of? which properly checks inheritance
    actual.kind_of?(@expected_class)
  end

  def failure_message(actual)
    # Handle both class objects (with .name) and strings
    class_name = @expected_class.name
    "expected #{actual.inspect} to be a kind of #{class_name}"
  end
end

def be_kind_of(expected_class)
  BeKindOfMatcher.new(expected_class)
end

# be_nil matcher
class BeNilMatcher
  def matches?(actual)
    actual == nil
  end

  def failure_message(actual)
    "expected #{actual.inspect} to be nil"
  end
end

def be_nil
  BeNilMatcher.new
end

# equal matcher (object identity)
class EqualMatcher
  def initialize(expected)
    @expected = expected
  end

  def matches?(actual)
    actual.equal?(@expected)
  end

  def failure_message(actual)
    "expected #{actual.inspect} to be the same object as #{@expected.inspect}"
  end

  def negative_failure_message(actual)
    "expected #{actual.inspect} to not be the same object as #{@expected.inspect}"
  end
end

def equal(expected)
  EqualMatcher.new(expected)
end

# be_true matcher
class BeTrueMatcher
  def matches?(actual)
    actual == true
  end

  def failure_message(actual)
    "expected #{actual.inspect} to be true"
  end

  def negative_failure_message(actual)
    "expected #{actual.inspect} to not be true"
  end
end

def be_true
  BeTrueMatcher.new
end

# be_false matcher
class BeFalseMatcher
  def matches?(actual)
    actual == false
  end

  def failure_message(actual)
    "expected #{actual.inspect} to be false"
  end

  def negative_failure_message(actual)
    "expected #{actual.inspect} to not be false"
  end
end

def be_false
  BeFalseMatcher.new
end

# respond_to matcher
class RespondToMatcher
  def initialize(method_name)
    @method_name = method_name
  end

  def matches?(actual)
    actual.respond_to?(@method_name)
  end

  def failure_message(actual)
    "expected #{actual.inspect} to respond to #{@method_name}"
  end

  def negative_failure_message(actual)
    "expected #{actual.inspect} to not respond to #{@method_name}"
  end
end

def respond_to(method_name)
  RespondToMatcher.new(method_name)
end

# include matcher
class IncludeMatcher
  def initialize(expected)
    @expected = expected
  end

  def matches?(actual)
    actual.include?(@expected)
  end

  def failure_message(actual)
    "expected #{actual.inspect} to include #{@expected.inspect}"
  end

  def negative_failure_message(actual)
    "expected #{actual.inspect} not to include #{@expected.inspect}"
  end
end

def include(expected)
  IncludeMatcher.new(expected)
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

  def equal?(expected)
    if !@actual.equal?(expected)
      raise("expected #{expected.inspect} but got #{@actual.inspect}")
    end
    true
  end
end

class ShouldNotExpectation
  def initialize(actual)
    @actual = actual
  end

  def ==(expected)
    if @actual == expected
      raise("expected #{@actual.inspect} to not equal #{expected.inspect}")
    end
    true
  end

  def be_kind_of(expected_class)
    if @actual.class.name == expected_class.name
      raise("expected #{@actual.inspect} to not be a kind of #{expected_class.name}")
    end
    true
  end

  def be_nil
    if @actual == nil
      raise("expected #{@actual.inspect} to not be nil")
    end
    true
  end

  def equal(expected)
    if @actual.equal?(expected)
      raise("expected #{@actual.inspect} to not be the same object as #{expected.inspect}")
    end
    true
  end
end

class Integer
  def should(matcher = nil)
    ShouldExpectation.new(self)
  end

  def should_not
    ShouldNotExpectation.new(self)
  end
end

class Float
  def should(matcher = nil)
    ShouldExpectation.new(self)
  end

  def should_not
    ShouldNotExpectation.new(self)
  end
end

class String
  def should(matcher = nil)
    ShouldExpectation.new(self)
  end

  def should_not
    ShouldNotExpectation.new(self)
  end
end

class Array
  def should(matcher = nil)
    ShouldExpectation.new(self)
  end

  def should_not(matcher = nil)
    if matcher != nil
      if matcher.matches?(self)
        raise(matcher.negative_failure_message(self))
      end
      true
    else
      ShouldNotExpectation.new(self)
    end
  end
end

class NilClass
  def should(matcher = nil)
    ShouldExpectation.new(self)
  end

  def should_not
    ShouldNotExpectation.new(self)
  end
end

class TrueClass
  def should(matcher = nil)
    if matcher != nil
      if !matcher.matches?(self)
        raise(matcher.failure_message(self))
      end
      true
    else
      ShouldExpectation.new(self)
    end
  end

  def should_not(matcher = nil)
    if matcher != nil
      if matcher.matches?(self)
        raise(matcher.negative_failure_message(self))
      end
      true
    else
      ShouldNotExpectation.new(self)
    end
  end
end

class FalseClass
  def should(matcher = nil)
    if matcher != nil
      if !matcher.matches?(self)
        raise(matcher.failure_message(self))
      end
      true
    else
      ShouldExpectation.new(self)
    end
  end

  def should_not(matcher = nil)
    if matcher != nil
      if matcher.matches?(self)
        raise(matcher.negative_failure_message(self))
      end
      true
    else
      ShouldNotExpectation.new(self)
    end
  end
end

class Symbol
  def should(matcher = nil)
    if matcher != nil
      if !matcher.matches?(self)
        raise(matcher.failure_message(self))
      end
      true
    else
      ShouldExpectation.new(self)
    end
  end

  def should_not(matcher = nil)
    if matcher != nil
      if matcher.matches?(self)
        raise(matcher.negative_failure_message(self))
      end
      true
    else
      ShouldNotExpectation.new(self)
    end
  end
end

class Object
  def should(matcher = nil)
    if matcher != nil
      if !matcher.matches?(self)
        raise(matcher.failure_message(self))
      end
      true
    else
      ShouldExpectation.new(self)
    end
  end

  def should_not(matcher = nil)
    if matcher != nil
      if matcher.matches?(self)
        raise(matcher.negative_failure_message(self))
      end
      true
    else
      ShouldNotExpectation.new(self)
    end
  end
end


# --------------------------
# raise_error matcher
# --------------------------

class RaiseErrorMatcher
  def initialize(expected_error, expected_message = nil)
    @expected_error = expected_error
    @expected_message = expected_message
    @has_expected = false
    # Store the class name for comparison
    if expected_error
      @has_expected = true
      @expected_name = expected_error.name
    end
  end

  def matches?(actual_error)
    # Check if error class matches
    if @has_expected
      # Compare class names as strings
      if actual_error.class.name != @expected_name
        return false
      end
    end

    # If message is specified, also check message
    if @expected_message
      actual_msg = actual_error.message.to_s
      expected_msg = @expected_message.to_s
      # Compare as strings
      if actual_msg == expected_msg
        return true
      else
        return false
      end
    end

    true
  end

  def failure_message_for_no_error
    "expected #{@expected_name} to be raised, but nothing was raised"
  end

  def failure_message_for_wrong_error(actual_error)
    if @expected_message
      "expected #{@expected_name} with message '#{@expected_message}' but got #{actual_error.class.name} with message '#{actual_error.message}'"
    else
      "expected #{@expected_name} but got #{actual_error.class.name}"
    end
  end
end

def raise_error(expected_error, expected_message = nil)
  RaiseErrorMatcher.new(expected_error, expected_message)
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
  begin
    load(test_file)
  rescue => e
    puts("ERROR loading test file: #{e.message}")
    $test_results << "ERROR"
  end
else
  puts("Usage: lein run test_rspec_capabilities.rb <test_file.rb>")
  puts("No test file provided.")
end

puts("")

total = $test_results.length
pass_count = 0
fail_count = 0
error_count = 0
i = 0

while i < total
  if $test_results[i] == "PASS"
    pass_count = pass_count + 1
  end
  if $test_results[i] == "FAIL"
    fail_count = fail_count + 1
  end
  if $test_results[i] == "ERROR"
    error_count = error_count + 1
  end
  i = i + 1
end

puts("#{pass_count} pass, #{fail_count} fail, #{error_count} error")

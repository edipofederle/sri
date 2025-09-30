# expected-output: Hello from class method

class Test
  def self.hello
    puts("Hello from class method")
  end
end

Test.hello
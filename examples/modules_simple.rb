# expected-output:  === Simple Module Example ===
# expected-output:  Greeting module created
# expected-output:  Greeting is a: Module
# expected-output:  Company module created
# expected-output:  Company is a: Module
# expected-output:  Company::HR is a: Module
# expected-output:  Company::IT is a: Module
# expected-output:  Greeting module reopened and extended
# expected-output:  Greeting is still a: Module
# expected-output:  === Module example complete ===

puts("=== Simple Module Example ===")

# 1. Basic module definition
module Greeting
  def say_hello
    puts("Hello from Greeting module!")
  end

  def say_goodbye
    puts("Goodbye from Greeting module!")
  end
end

puts("Greeting module created")
puts("Greeting is a: " + Greeting.class)

# 2. Nested modules
module Company
  module HR
    def hire_employee
      puts("Employee hired!")
    end
  end

  module IT
    def setup_computer
      puts("Computer setup complete!")
    end
  end
end

puts("Company module created")
puts("Company is a: " + Company.class)
puts("Company::HR is a: " + Company::HR.class)
puts("Company::IT is a: " + Company::IT.class)

# 3. Module reopening
module Greeting
  def say_welcome
    puts("Welcome from Greeting module!")
  end
end

puts("Greeting module reopened and extended")
puts("Greeting is still a: " + Greeting.class)

puts("=== Module example complete ===")

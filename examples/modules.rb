# expected-output:  === Ruby Modules Example ===
# expected-output:  1. Basic Module:
# expected-output:  Module
# expected-output:  2. Module with Methods:
# expected-output:  Module
# expected-output:  Defining Company module...
# expected-output:  Defining Department module...
# expected-output:  Defining Team module...
# expected-output:  3. Nested Modules:
# expected-output:  Company class: Module
# expected-output:  Company::Department class: Module
# expected-output:  Company::Department::Team class: Module
# expected-output:  4. Module before reopening:
# expected-output:  Module
# expected-output:  4. Module after reopening:
# expected-output:  Module
# expected-output:  5. Modules as Namespaces:
# expected-output:  Math::Geometry::Circle class: Module
# expected-output:  Math::Geometry::Rectangle class: Module
# expected-output:  Math::Algebra class: Module
# expected-output:  6. Module Constants:
# expected-output:  Config class: Module
# expected-output:  Config::Database class: Module
# expected-output:  === Module Features Summary ===
# expected-output:  ✓ Basic module definition
# expected-output:  ✓ Modules with methods
# expected-output:  ✓ Nested modules with :: syntax
# expected-output:  ✓ Module reopening
# expected-output:  ✓ Modules as namespaces
# expected-output:  ✓ Module constants and scope
# expected-output:  ✓ All modules are Module class instances
# expected-output:  === End of Module Example ===

puts("=== Ruby Modules Example ===")

# 1. Basic Module Definition
module BasicModule
  def self.info
    puts("This is BasicModule")
  end
end

puts("1. Basic Module:")
puts(BasicModule.class)

# 2. Module with Instance Methods
module Greetings
  def hello
    puts("Hello from Greetings module!")
  end

  def goodbye
    puts("Goodbye from Greetings module!")
  end
end

puts("2. Module with Methods:")
puts(Greetings.class)

# 3. Nested Modules
module Company
  puts("Defining Company module...")

  module Department
    puts("Defining Department module...")

    def department_info
      puts("This is a department")
    end

    module Team
      puts("Defining Team module...")

      def team_info
        puts("This is a team within a department")
      end
    end
  end

  def company_info
    puts("This is the company")
  end
end

puts("3. Nested Modules:")
puts("Company class: " + Company.class)
puts("Company::Department class: " + Company::Department.class)
puts("Company::Department::Team class: " + Company::Department::Team.class)

# 4. Module Reopening (defining same module multiple times)
module ReopenExample
  def method1
    puts("Method 1 from first definition")
  end
end

puts("4. Module before reopening:")
puts(ReopenExample.class)

# Reopen the module to add more methods
module ReopenExample
  def method2
    puts("Method 2 from second definition")
  end

  def method3
    puts("Method 3 from second definition")
  end
end

puts("4. Module after reopening:")
puts(ReopenExample.class)

# 5. Modules as Namespaces
module Math
  module Geometry
    module Circle
      def self.area(radius)
        puts("Circle area calculation would go here")
        puts("Area for radius " + radius.to_s() + " = " + (3.14 * radius * radius).to_s())
      end
    end

    module Rectangle
      def self.area(width, height)
        puts("Rectangle area: " + (width * height).to_s())
      end
    end
  end

  module Algebra
    def self.quadratic_formula
      puts("Quadratic formula: (-b ± √(b²-4ac)) / 2a")
    end
  end
end

puts("5. Modules as Namespaces:")
puts("Math::Geometry::Circle class: " + Math::Geometry::Circle.class)
puts("Math::Geometry::Rectangle class: " + Math::Geometry::Rectangle.class)
puts("Math::Algebra class: " + Math::Algebra.class)

# 6. Module Constants and Scope
module Config
  VERSION = "1.0.0"
  DEBUG = true

  module Database
    HOST = "localhost"
    PORT = 5432

    def self.connection_string
      puts("Database connection: " + HOST + ":" + PORT.to_s())
    end
  end
end

puts("6. Module Constants:")
puts("Config class: " + Config.class)
puts("Config::Database class: " + Config::Database.class)

puts("=== Module Features Summary ===")
puts("✓ Basic module definition")
puts("✓ Modules with methods")
puts("✓ Nested modules with :: syntax")
puts("✓ Module reopening")
puts("✓ Modules as namespaces")
puts("✓ Module constants and scope")
puts("✓ All modules are Module class instances")

puts("=== End of Module Example ===")

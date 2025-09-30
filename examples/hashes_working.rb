# Demonstrates hash literals, operations, and methods

puts("=== Hash Literals ===")

# Empty hash
empty_hash = {}
puts("Empty hash:")
puts(empty_hash)

# Hash with string keys and mixed values
person = {"name" => "Alice", "age" => 30, "city" => "New York"}
puts("Person hash:")
puts(person)

# Hash with integer keys
scores = {1 => "A", 2 => "B", 3 => "C"}
puts("Scores hash:")
puts(scores)

puts("=== Hash Access ===")

# Reading values by key
puts("Name:")
puts(person["name"])
puts("Age:")
puts(person["age"])

# Accessing non-existent key returns nil
puts("Height (missing key):")
puts(person["height"])

puts("=== Hash Assignment ===")

# Adding new key-value pairs
person["country"] = "USA"
person["occupation"] = "Engineer"
puts("After adding country and occupation:")
puts(person)

# Modifying existing values
person["age"] = 31
puts("After updating age:")
puts(person)

puts("=== Hash Methods ===")

# Size and emptiness
puts("Hash size:")
puts(person.size())
puts("Hash length:")
puts(person.length())
puts("Is empty?")
puts(person.empty?())

# Key checking methods
puts("Key name exists?")
puts(person.key?("name"))
puts("Includes city?")
puts(person.include?("city"))
puts("Key salary missing?")
puts(person.key?("salary"))

puts("=== Hash Modification ===")

# Removing key-value pairs
puts("Before deletion:")
puts(person)
person.delete("country")
puts("After deleting country:")
puts(person)

# Using remove (alternative method)
person.remove("occupation")
puts("After removing occupation:")
puts(person)

puts("=== Hash Use Cases ===")

# Configuration hash
config = {"debug" => true, "timeout" => 5000, "host" => "localhost"}
puts("Configuration:")
puts(config)

# Using hash as a lookup table
status_codes = {200 => "OK", 404 => "Not Found", 500 => "Internal Server Error"}
puts("HTTP status for 404:")
puts(status_codes[404])

puts("=== Hash Features Summary ===")
puts("Hash literals: {key => value}")
puts("Hash access: hash[key]")
puts("Hash assignment: hash[key] = value")
puts("Ruby-style methods: size, empty?, has_key?, key?, include?, member?")
puts("Java-style methods: length, isEmpty, remove")
puts("Key/value extraction: keys(), values()")
puts("Hash modification: delete(key), remove(key)")
puts("Mixed key types: strings, integers")
puts("Mixed value types: strings, integers, booleans")
puts("Hash implementation complete!")

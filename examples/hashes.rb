# expected-output: Empty hash:
# expected-output: {}
# expected-output: Person hash:
# expected-output: {"name"=>"Alice", "age"=>30, "city"=>"New York"}
# expected-output: Scores hash:
# expected-output: {1=>"A", 2=>"B", 3=>"C"}
# expected-output: Alice
# expected-output: 30
# expected-output: nil
# expected-output: {"name"=>"Alice", "age"=>30, "city"=>"New York", "country"=>"BR", "occupation"=>"Engineer"}
# expected-output: After updating age:
# expected-output: {"name"=>"Alice", "age"=>31, "city"=>"New York", "country"=>"BR", "occupation"=>"Engineer"}
# expected-output: Hash size:
# expected-output: 5
# expected-output: Hash length:
# expected-output: 5
# expected-output: Is empty?
# expected-output: false
# expected-output: Has name key:
# expected-output: true
# expected-output: Has age key:
# expected-output: true
# expected-output: true
# expected-output: true
# expected-output: name
# expected-output: age
# expected-output: city
# expected-output: country
# expected-output: occupation
# expected-output: Alice
# expected-output: 31
# expected-output: New York
# expected-output: BR
# expected-output: Engineer
# expected-output: {"name"=>"Alice", "age"=>31, "city"=>"New York", "country"=>"BR", "occupation"=>"Engineer"}
# expected-output: {"name"=>"Alice", "age"=>31, "city"=>"New York", "occupation"=>"Engineer"}
# expected-output: {"name"=>"Alice", "age"=>31, "city"=>"New York"}
# expected-output: Configuration hash:
# expected-output: {"debug"=>true, "timeout"=>5000, "host"=>"localhost"}
# expected-output: Status code 404:
# expected-output: Not Found

# Empty hash
empty_hash = {}
puts("Empty hash:")
puts(empty_hash)

# # Hash with string keys and mixed values
person = {"name" => "Alice", "age" => 30, "city" => "New York"}
puts("Person hash:")
puts(person)

# # Hash with integer keys
scores = {1 => "A", 2 => "B", 3 => "C"}
puts("Scores hash:")
puts(scores)

puts(person["name"])
puts(person["age"])


puts(person["height"])

person["country"] = "BR"
person["occupation"] = "Engineer"
puts(person)

# Modifying existing values
person["age"] = 31
puts("After updating age:")
puts(person)


# Size and emptiness
puts("Hash size:")
puts(person.size())
puts("Hash length:")
puts(person.length())
puts("Is empty?")
puts(person.empty?())

# Key checking methods (existing keys work perfectly)
puts("Has name key:")
puts(person.key?("name"))
puts("Has age key:")
puts(person.include?("age"))
puts(person.include?("city"))
puts(person.member?("city"))

#puts(person.member?("salary")) # not working, key dont exit

# Getting keys and values
puts(person.keys())
puts(person.values())




puts(person)
person.delete("country")
puts(person)


person.remove("occupation")
puts(person)


# Configuration hash (single-line only - multi-line parsing not yet supported)
config = {"debug" => true, "timeout" => 5000, "host" => "localhost"}

puts("Configuration hash:")
puts(config)

# Using hash as a lookup table (single-line)
status_codes = {200 => "OK", 404 => "Not Found", 500 => "Internal Server Error"}
puts("Status code 404:")
puts(status_codes[404])

# # Nested data structure (hash as value)
# user = {
#   "id" => 123,
#   "profile" => {"first_name" => "Bob", "last_name" => "Smith"}
# }

# puts(user)

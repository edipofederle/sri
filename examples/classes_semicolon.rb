# expected-output: --- Creating instances ---
# expected-output: Creating a new person
# expected-output:--- Done ---


class Person; def initialize; puts("Creating a new person"); end; end; puts("--- Creating instances ---"); person = Person.new; puts("--- Done ---")
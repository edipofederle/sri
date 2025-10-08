# expected-output: Account holder: John Doe
# expected-output: Account number: 12345
# expected-output: Initial balance: 1000
# expected-output: New owner: Jane Smith
# expected-output: Deposited 500. New balance: 1500
# expected-output: Withdrew 200. New balance: 1300
# expected-output: Final balance: 1300

class BankAccount
  attr_reader :balance, :account_number
  attr_writer :pin
  attr_accessor :owner

  def initialize(owner, account_number, initial_balance)
    @owner = owner
    @account_number = account_number
    @balance = initial_balance
    @pin = "0000"
  end

  def deposit(amount)
    @balance = @balance + amount
    puts("Deposited " + amount.to_s + ". New balance: " + @balance.to_s)
  end

  def withdraw(amount)
    if @balance >= amount
      @balance = @balance - amount
      puts("Withdrew " + amount.to_s + ". New balance: " + @balance.to_s)
    else
      puts("Insufficient funds")
    end
  end
end

# Create account
account = BankAccount.new("John Doe", 12345, 1000)

# Test attr_reader (can read but not write)
puts("Account holder: " + account.owner)
puts("Account number: " + account.account_number.to_s)
puts("Initial balance: " + account.balance.to_s)

# Test attr_writer (can write but not read)
account.pin = "1234"  # This works
# puts(account.pin)   # This would fail - no getter for pin

# Test attr_accessor (can both read and write)
account.owner = "Jane Smith"
puts("New owner: " + account.owner)

# Test methods that modify balance
account.deposit(500)
account.withdraw(200)
puts("Final balance: " + account.balance.to_s)

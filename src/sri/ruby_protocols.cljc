(ns sri.ruby-protocols
  "Core protocols for Ruby object system.")

(defprotocol RubyObject
  "Core protocol that all Ruby objects must implement."
  (ruby-class [this] "Returns the Ruby class name as a string")
  (ruby-ancestors [this] "Returns the ancestor chain as a vector of class names")
  (respond-to? [this method-name] "Returns true if object responds to method")
  (get-ruby-method [this method-name] "Gets the method implementation for this object"))

(defprotocol RubyInspectable
  "Protocol for string representation of Ruby objects."
  (to-s [this] "Returns string representation (Ruby .to_s)")
  (inspect [this] "Returns debug string representation (Ruby .inspect)"))

(defprotocol RubyComparable
  "Protocol for comparison operations."
  (ruby-eq [this other] "Ruby == comparison")
  (ruby-compare [this other] "Ruby <=> comparison (-1, 0, 1)"))

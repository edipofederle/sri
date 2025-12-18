(ns sri.ruby-classes-new
  "Ruby Class Hierarchy - Modular Implementation

   This namespace provides a unified interface to Ruby's object-oriented model,
   implementing the complete Ruby class hierarchy using Clojure protocols and records.

   ARCHITECTURE
   ============
   This is a modular refactor where each Ruby class type is implemented in its own
   namespace. This file serves as the main public API that re-exports functionality
   from specialized modules:

   - sri.ruby-protocols      : Core protocols (RubyObject, RubyInspectable, etc.)
   - sri.ruby-method-registry: Method lookup and dispatch system
   - sri.ruby-basic-object   : BasicObject (root of Ruby hierarchy)
   - sri.ruby-object         : Object and Kernel functionality
   - sri.ruby-string         : String class implementation
   - sri.ruby-array          : Array class with enumerable support
   - sri.ruby-range          : Range objects (numeric and character)
   - sri.ruby-hash           : Hash/dictionary implementation
   - sri.ruby-numeric        : Numeric types (Integer, Float)
   - sri.ruby-rational       : Rational number implementation
   - sri.ruby-complex        : Complex number implementation

   RUBY CLASS HIERARCHY
   ====================
   BasicObject
   └── Object (includes Kernel)
       ├── String
       ├── Numeric
       │   ├── Integer
       │   ├── Float
       │   ├── Rational
       │   └── Complex
       ├── Array
       ├── Hash
       └── Range

   USAGE
   =====
   Basic object creation:
   ```clojure
   (require '[sri.ruby-classes-new :as ruby])

   ;; Create Ruby objects
   (def str (ruby/create-string \"Hello\"))
   (def arr (ruby/create-array [1 2 3]))
   (def range (ruby/create-range 1 5))

   ;; Call Ruby methods
   (ruby/invoke-ruby-method str :upcase)     ; => \"HELLO\"
   (ruby/invoke-ruby-method arr :length)     ; => 3
   (ruby/invoke-ruby-method range :each ...)  ; iterate over range
   ```

   Method introspection:
   ```clojure
   ;; Check if object responds to method
   (ruby/respond-to? str :upcase)  ; => true

   ;; Get object's Ruby class
   (ruby/ruby-class str)           ; => \"String\"

   ;; Get ancestor chain
   (ruby/ruby-ancestors str)       ; => [\"String\" \"Object\" \"Kernel\" \"BasicObject\"]
   ```

   Low-level access:
   ```clojure
   ;; Direct record construction (advanced usage)
   (ruby/->RubyString \"test\")

   ;; Method registry access
   (ruby/has-method? \"String\" :upcase)
   (ruby/class-methods \"String\")
   ```"
  (:require [sri.ruby-protocols :as protocols]
            [sri.ruby-method-registry :as registry]
            [sri.ruby-basic-object :as basic-obj]
            [sri.ruby-object :as ruby-obj]
            [sri.ruby-string :as ruby-str]
            [sri.ruby-range :as ruby-range]
            [sri.ruby-array :as ruby-array]
            [sri.ruby-hash :as ruby-hash]
            [sri.ruby-rational :as ruby-rational]
            [sri.ruby-complex :as ruby-complex]
            [sri.ruby-numeric :as ruby-numeric]))

;; =============================================================================
;; Re-export Constructor Functions
;; =============================================================================

;; BasicObject
(def create-basic-object
  "Creates a new BasicObject instance - the root of Ruby's class hierarchy.
   BasicObject provides minimal functionality and is the ancestor of all Ruby objects.

   Returns: A new BasicObject instance

   Example:
     (def obj (create-basic-object))
     (ruby-class obj)  ; => \"BasicObject\""
  basic-obj/create-basic-object)

;; Object
(def create-object
  "Creates a new Object instance with optional value.
   Object includes Kernel module and provides core Ruby functionality.

   Args:
     value (optional): The value to wrap in the Object

   Returns: A new RubyObjectClass instance

   Examples:
     (create-object)        ; => Object with nil value
     (create-object 42)     ; => Object wrapping 42"
  ruby-obj/create-object)

(def ruby-nil
  "Creates a Ruby nil object (Object with nil value).

   Returns: RubyObjectClass representing Ruby's nil

   Example:
     (def nil-obj (ruby-nil))
     (invoke-ruby-method nil-obj :nil?)  ; => true"
  ruby-obj/ruby-nil)

(def ruby-true
  "Creates a Ruby true object (Object with true value).

   Returns: RubyObjectClass representing Ruby's true

   Example:
     (def true-obj (ruby-true))
     (invoke-ruby-method true-obj :nil?)  ; => false"
  ruby-obj/ruby-true)

(def ruby-false
  "Creates a Ruby false object (Object with false value).

   Returns: RubyObjectClass representing Ruby's false

   Example:
     (def false-obj (ruby-false))
     (invoke-ruby-method false-obj :nil?)  ; => false"
  ruby-obj/ruby-false)

;; String
(def create-string
  "Creates a new Ruby String object from the given value.

   Args:
     value: The string value (will be converted to string)

   Returns: A new RubyString instance

   Examples:
     (create-string \"hello\")     ; => RubyString \"hello\"
     (create-string 123)       ; => RubyString \"123\"

     (def s (create-string \"test\"))
     (invoke-ruby-method s :upcase)  ; => \"TEST\""
  ruby-str/create-string)

;; Range
(def create-range
  "Creates a new Ruby Range object.

   Args:
     start: The starting value of the range
     end: The ending value of the range
     exclusive? (optional): If true, excludes the end value (default false)

   Returns: A new RubyRange instance

   Examples:
     (create-range 1 5)        ; => Range 1..5 (includes 5)
     (create-range 1 5 true)   ; => Range 1...5 (excludes 5)
     (create-range \"a\" \"z\")     ; => Character range a..z"
  ruby-range/create-range)

(def ruby-range?
  "Checks if the given object is a Ruby Range.

   Args:
     obj: Object to check

   Returns: true if obj is a RubyRange, false otherwise

   Example:
     (ruby-range? (create-range 1 5))  ; => true
     (ruby-range? [1 2 3])             ; => false"
  ruby-range/ruby-range?)

(def char-range?
  "Checks if the given range is a character range.

   Args:
     range: RubyRange object to check

   Returns: true if range contains characters, false otherwise

   Example:
     (char-range? (create-range \"a\" \"z\"))  ; => true
     (char-range? (create-range 1 5))      ; => false"
  ruby-range/char-range?)

(def char-to-int
  "Converts a character to its ASCII/Unicode integer value.

   Args:
     ch: Character to convert

   Returns: Integer representation of the character

   Example:
     (char-to-int \\a)  ; => 97"
  ruby-range/char-to-int)

(def int-to-char
  "Converts an integer to its corresponding character.

   Args:
     i: Integer to convert

   Returns: Character representation of the integer

   Example:
     (int-to-char 97)  ; => \\a"
  ruby-range/int-to-char)

;; Array
(def create-array
  "Creates a new Ruby Array from a collection.

   Args:
     coll: A Clojure collection to convert to Ruby Array

   Returns: A new RubyArray instance

   Example:
     (create-array [1 2 3])  ; => RubyArray [1, 2, 3]
     (create-array '(\"a\" \"b\"))   ; => RubyArray [\"a\", \"b\"]"
  ruby-array/create-array)

(def create-empty-array
  "Creates a new empty Ruby Array.

   Returns: A new empty RubyArray instance

   Example:
     (def arr (create-empty-array))
     (invoke-ruby-method arr :empty?)  ; => true"
  ruby-array/create-empty-array)

(def vector->ruby-array
  "Converts a Clojure vector to a Ruby Array.

   Args:
     vec: Clojure vector to convert

   Returns: RubyArray containing the vector's elements

   Example:
     (vector->ruby-array [1 2 3])  ; => RubyArray [1, 2, 3]"
  ruby-array/vector->ruby-array)

(def ruby-array?
  "Checks if the given object is a Ruby Array.

   Args:
     obj: Object to check

   Returns: true if obj is a RubyArray, false otherwise

   Example:
     (ruby-array? (create-array [1 2 3]))  ; => true
     (ruby-array? [1 2 3])                 ; => false"
  ruby-array/ruby-array?)

(def mutable-array?
  "Checks if the given Ruby Array is mutable.

   Args:
     arr: RubyArray to check

   Returns: true if array supports mutation, false otherwise"
  ruby-array/mutable-array?)

(def array-get
  "Gets an element from a Ruby Array by index.

   Args:
     arr: RubyArray instance
     index: Integer index

   Returns: Element at the given index, or nil if out of bounds

   Example:
     (def arr (create-array [\"a\" \"b\" \"c\"]))
     (array-get arr 1)  ; => \"b\""
  ruby-array/array-get)

(def array-set!
  "Sets an element in a Ruby Array at the given index.

   Args:
     arr: RubyArray instance (must be mutable)
     index: Integer index
     value: Value to set

   Returns: The set value

   Example:
     (def arr (create-array [1 2 3]))
     (array-set! arr 1 \"new\")  ; arr now contains [1, \"new\", 3]"
  ruby-array/array-set!)

(def array-push!
  "Appends an element to the end of a Ruby Array.

   Args:
     arr: RubyArray instance (must be mutable)
     value: Value to append

   Returns: The modified array

   Example:
     (def arr (create-array [1 2]))
     (array-push! arr 3)  ; arr now contains [1, 2, 3]"
  ruby-array/array-push!)

(def array-pop!
  "Removes and returns the last element from a Ruby Array.

   Args:
     arr: RubyArray instance (must be mutable)

   Returns: The removed element, or nil if array is empty

   Example:
     (def arr (create-array [1 2 3]))
     (array-pop! arr)  ; => 3, arr now contains [1, 2]"
  ruby-array/array-pop!)

;; Hash
(def create-hash
  "Creates a new Ruby Hash from key-value pairs or a map.

   Args:
     data: Map or sequence of key-value pairs

   Returns: A new RubyHash instance

   Examples:
     (create-hash {:a 1 :b 2})        ; => RubyHash {\"a\" => 1, \"b\" => 2}
     (create-hash [[\"x\" 10] [\"y\" 20]])  ; => RubyHash {\"x\" => 10, \"y\" => 20}"
  ruby-hash/create-hash)

(def create-empty-hash
  "Creates a new empty Ruby Hash.

   Returns: A new empty RubyHash instance

   Example:
     (def h (create-empty-hash))
     (invoke-ruby-method h :empty?)  ; => true"
  ruby-hash/create-empty-hash)

(def map->ruby-hash
  "Converts a Clojure map to a Ruby Hash.

   Args:
     m: Clojure map to convert

   Returns: RubyHash containing the map's key-value pairs

   Example:
     (map->ruby-hash {:name \"Alice\" :age 30})  ; => RubyHash {\"name\" => \"Alice\", \"age\" => 30}"
  ruby-hash/map->ruby-hash)

(def ruby-hash?
  "Checks if the given object is a Ruby Hash.

   Args:
     obj: Object to check

   Returns: true if obj is a RubyHash, false otherwise

   Example:
     (ruby-hash? (create-hash {:a 1}))  ; => true
     (ruby-hash? {:a 1})                ; => false"
  ruby-hash/ruby-hash?)

;; Rational
(defn create-rational
  "Creates a new Ruby Rational number.

   Args:
     numerator: The numerator (integer)
     denominator: The denominator (integer, must be non-zero)

   Returns: A new RubyRational instance

   Examples:
     (create-rational 3 4)   ; => Rational 3/4
     (create-rational 22 7)  ; => Rational 22/7 (approx π)

     (def r (create-rational 1 2))
     (invoke-ruby-method r :to_s)  ; => \"1/2\""
  [numerator denominator]
  (ruby-rational/->RubyRational numerator denominator))

(def ruby-rational?
  "Checks if the given object is a Ruby Rational number.

   Args:
     obj: Object to check

   Returns: true if obj is a RubyRational, false otherwise

   Example:
     (ruby-rational? (create-rational 1 2))  ; => true
     (ruby-rational? 0.5)                    ; => false"
  ruby-rational/ruby-rational?)

;; Complex
(defn create-complex
  "Creates a new Ruby Complex number.

   Args:
     real: The real part (number)
     imaginary: The imaginary part (number)

   Returns: A new RubyComplex instance

   Examples:
     (create-complex 3 4)    ; => Complex 3+4i
     (create-complex 1 0)    ; => Complex 1+0i (real number)
     (create-complex 0 1)    ; => Complex 0+1i (pure imaginary)

     (def c (create-complex 2 3))
     (invoke-ruby-method c :to_s)  ; => \"2+3i\""
  [real imaginary]
  (ruby-complex/->RubyComplex real imaginary))

(def ruby-complex?
  "Checks if the given object is a Ruby Complex number.

   Args:
     obj: Object to check

   Returns: true if obj is a RubyComplex, false otherwise

   Example:
     (ruby-complex? (create-complex 1 2))  ; => true
     (ruby-complex? 42)                    ; => false"
  ruby-complex/ruby-complex?)

;; Numeric
(def ruby-numeric?
  "Checks if the given object is a Ruby numeric type.

   Args:
     obj: Object to check

   Returns: true if obj is any Ruby numeric type (Integer, Float, Rational, Complex), false otherwise

   Examples:
     (ruby-numeric? 42)                    ; => true
     (ruby-numeric? 3.14)                  ; => true
     (ruby-numeric? (create-rational 1 2)) ; => true
     (ruby-numeric? \"42\")                  ; => false"
  ruby-numeric/ruby-numeric?)

(def ruby-integer?
  "Checks if the given object is a Ruby Integer.

   Args:
     obj: Object to check

   Returns: true if obj is a Ruby Integer, false otherwise

   Examples:
     (ruby-integer? 42)    ; => true
     (ruby-integer? 3.14)  ; => false"
  ruby-numeric/ruby-integer?)

(def ruby-float?
  "Checks if the given object is a Ruby Float.

   Args:
     obj: Object to check

   Returns: true if obj is a Ruby Float, false otherwise

   Examples:
     (ruby-float? 3.14)  ; => true
     (ruby-float? 42)    ; => false"
  ruby-numeric/ruby-float?)

(def numeric-ruby-class
  "Gets the Ruby class name for a numeric value.

   Args:
     num: Numeric value

   Returns: String representing the Ruby class name

   Examples:
     (numeric-ruby-class 42)    ; => \"Integer\"
     (numeric-ruby-class 3.14)  ; => \"Float\""
  ruby-numeric/numeric-ruby-class)

(def numeric-ruby-ancestors
  "Gets the Ruby ancestor chain for a numeric value.

   Args:
     num: Numeric value

   Returns: Vector of ancestor class names

   Example:
     (numeric-ruby-ancestors 42)  ; => [\"Integer\" \"Numeric\" \"Object\" \"Kernel\" \"BasicObject\"]"
  ruby-numeric/numeric-ruby-ancestors)

;; =============================================================================
;; Re-export Record Constructors (Advanced/Internal Use)
;; =============================================================================

(def ->BasicObject
  "Direct record constructor for BasicObject.
   Advanced usage - prefer create-basic-object for normal use."
  basic-obj/->BasicObject)

(def ->RubyObjectClass
  "Direct record constructor for RubyObjectClass.
   Advanced usage - prefer create-object for normal use."
  ruby-obj/->RubyObjectClass)

(def ->RubyString
  "Direct record constructor for RubyString.
   Advanced usage - prefer create-string for normal use."
  ruby-str/->RubyString)

(def ->RubyRange
  "Direct record constructor for RubyRange.
   Advanced usage - prefer create-range for normal use."
  ruby-range/->RubyRange)

(def ->RubyArray
  "Direct record constructor for RubyArray.
   Advanced usage - prefer create-array for normal use."
  ruby-array/->RubyArray)

(def ->RubyHash
  "Direct record constructor for RubyHash.
   Advanced usage - prefer create-hash for normal use."
  ruby-hash/->RubyHash)

(def ->RubyRational
  "Direct record constructor for RubyRational.
   Advanced usage - prefer create-rational for normal use."
  ruby-rational/->RubyRational)

(def ->RubyComplex
  "Direct record constructor for RubyComplex.
   Advanced usage - prefer create-complex for normal use."
  ruby-complex/->RubyComplex)

;; =============================================================================
;; Method Call Interface
;; =============================================================================

(defn invoke-ruby-method
  "Main interface for calling Ruby methods on objects.

   This is the primary function for invoking Ruby methods with proper method lookup
   through the ancestor chain. Supports all Ruby objects created by this library.

   Args:
     obj: Ruby object (must satisfy RubyObject protocol)
     method-name: Method name (keyword or string)
     args: Variable arguments to pass to the method

   Returns: Result of the method call

   Throws: ex-info if object doesn't satisfy RubyObject or method not found

   Examples:
     (def s (create-string \"hello\"))
     (invoke-ruby-method s :upcase)           ; => \"HELLO\"
     (invoke-ruby-method s :+ \" world\")       ; => \"hello world\"
     (invoke-ruby-method s :slice 1 3)       ; => \"ell\"

     (def arr (create-array [1 2 3]))
     (invoke-ruby-method arr :length)         ; => 3
     (invoke-ruby-method arr :push 4)         ; => [1, 2, 3, 4]

     ;; Method introspection
     (invoke-ruby-method s :respond_to? :upcase)  ; => true
     (invoke-ruby-method s :class)                ; => \"String\""
  [obj method-name & args]
  (cond
    (satisfies? protocols/RubyObject obj)
    (apply registry/call-ruby-method obj method-name args)

    ;; Fallback for non-Ruby objects (backward compatibility)
    :else
    (throw (ex-info (str "Cannot call method " method-name " on non-Ruby object")
                    {:object obj :method method-name :args args}))))

;; =============================================================================
;; Re-export Protocol Functions
;; =============================================================================

(def ruby-class
  "Gets the Ruby class name of an object.

   Args:
     obj: Ruby object

   Returns: String representing the Ruby class name

   Examples:
     (ruby-class (create-string \"test\"))  ; => \"String\"
     (ruby-class (create-array [1 2]))     ; => \"Array\""
  protocols/ruby-class)

(def ruby-ancestors
  "Gets the ancestor chain of a Ruby object.

   Args:
     obj: Ruby object

   Returns: Vector of ancestor class names in method resolution order

   Example:
     (ruby-ancestors (create-string \"test\"))  ; => [\"String\" \"Object\" \"Kernel\" \"BasicObject\"]"
  protocols/ruby-ancestors)

(def respond-to?
  "Checks if an object responds to a given method.

   Args:
     obj: Ruby object
     method-name: Method name (keyword or string)

   Returns: true if object has the method, false otherwise

   Example:
     (respond-to? (create-string \"test\") :upcase)  ; => true
     (respond-to? (create-string \"test\") :foobar)  ; => false"
  protocols/respond-to?)

(def to-s
  "Converts a Ruby object to its string representation (Ruby's to_s method).

   Args:
     obj: Ruby object

   Returns: String representation of the object

   Example:
     (to-s (create-string \"hello\"))  ; => \"hello\"
     (to-s (create-array [1 2 3]))     ; => \"[1, 2, 3]\""
  protocols/to-s)

(def inspect
  "Gets the debug string representation of a Ruby object (Ruby's inspect method).

   Args:
     obj: Ruby object

   Returns: Debug string representation

   Example:
     (inspect (create-string \"hello\"))  ; => \"\\\"hello\\\"\""
  protocols/inspect)

(def ruby-eq
  "Performs Ruby equality comparison (==).

   Args:
     obj1: First Ruby object
     obj2: Second object

   Returns: true if objects are equal according to Ruby semantics

   Example:
     (ruby-eq (create-string \"hi\") (create-string \"hi\"))  ; => true"
  protocols/ruby-eq)

(def ruby-compare
  "Performs Ruby spaceship comparison (<=>).

   Args:
     obj1: First Ruby object
     obj2: Second object

   Returns: -1, 0, 1, or nil for less, equal, greater, or incomparable

   Example:
     (ruby-compare (create-string \"a\") (create-string \"b\"))  ; => -1"
  protocols/ruby-compare)

;; =============================================================================
;; Re-export Protocols (For Protocol Extension)
;; =============================================================================

(def RubyObject
  "Core protocol that all Ruby objects must implement.
   Defines ruby-class, ruby-ancestors, respond-to?, and get-ruby-method.

   Use this for extending the protocol to new types."
  protocols/RubyObject)

(def RubyInspectable
  "Protocol for string representation of Ruby objects.
   Defines to-s and inspect methods.

   Use this for extending string representation to new types."
  protocols/RubyInspectable)

(def RubyComparable
  "Protocol for comparison operations between Ruby objects.
   Defines ruby-eq and ruby-compare methods.

   Use this for extending comparison semantics to new types."
  protocols/RubyComparable)

;; =============================================================================
;; Re-export Method Registry Functions (Advanced/Internal Use)
;; =============================================================================

(def has-method?
  "Checks if a Ruby class has a specific method registered.

   Args:
     class-name: String name of the Ruby class
     method-name: Method name (keyword or string)

   Returns: true if method exists for the class

   Example:
     (has-method? \"String\" :upcase)  ; => true"
  registry/has-method?)

(def method-lookup
  "Looks up a method implementation in the ancestor chain.

   Args:
     obj: Ruby object
     method-name: Method name to find

   Returns: Method function or nil if not found

   Note: This is low-level - prefer invoke-ruby-method for normal use."
  registry/method-lookup)

(def call-ruby-method
  "Calls a method on a Ruby object with proper lookup.

   Args:
     obj: Ruby object
     method-name: Method name
     args: Method arguments

   Returns: Method result

   Note: This is low-level - prefer invoke-ruby-method for normal use."
  registry/call-ruby-method)

(def register-method
  "Registers a method implementation for a Ruby class.

   Args:
     class-name: String name of the Ruby class
     method-name: Method name (keyword or string)
     method-fn: Function implementing the method

   Use this to add new methods to existing Ruby classes.

   Example:
     (register-method \"String\" :reverse-upcase
                      #(-> % :value str/reverse str/upper-case create-string))"
  registry/register-method)

(def get-ruby-method-impl
  "Gets the method implementation for a specific class.

   Args:
     class-name: String name of the Ruby class
     method-name: Method name

   Returns: Method function or nil if not found"
  registry/get-ruby-method-impl)

;; =============================================================================
;; Re-export Debugging and Introspection
;; =============================================================================

(def debug-method-registry
  "Returns the complete method registry for debugging.

   Returns: Map of {class-name {method-name method-fn}}

   Useful for inspecting what methods are available."
  registry/debug-method-registry)

(def class-methods
  "Gets all method names available for a Ruby class.

   Args:
     class-name: String name of the Ruby class

   Returns: Collection of method names (keywords)

   Example:
     (class-methods \"String\")  ; => (:upcase :downcase :length :+ ...)"
  registry/class-methods)

(def all-ruby-classes
  "Gets all registered Ruby class names.

   Returns: Collection of class name strings

   Example:
     (all-ruby-classes)  ; => (\"BasicObject\" \"Object\" \"String\" \"Array\" ...)"
  registry/all-ruby-classes)

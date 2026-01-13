(ns sri.ruby-exception
  "Ruby Exception Class Implementation

   This namespace implements Ruby's exception hierarchy with proper inheritance,
   following the same patterns used for other Ruby classes in this interpreter.

   RUBY EXCEPTION HIERARCHY
   ========================
   Exception
   ├── StandardError (default for rescue)
   │   ├── RuntimeError
   │   ├── NoMethodError
   │   ├── ArgumentError
   │   ├── LocalJumpError
   │   ├── NameError
   │   └── TypeError
   ├── ScriptError
   │   ├── SyntaxError
   │   └── LoadError
   ├── SystemError
   └── SignalException

   USAGE
   =====
   ```clojure
   ;; Create exceptions
   (def runtime-error (create-runtime-error \"Something went wrong\"))
   (def no-method-error (create-no-method-error \"undefined method `foo`\"))

   ;; Exception methods
   (invoke-exception-method runtime-error :message [])     ; => \"Something went wrong\"
   (invoke-exception-method runtime-error :backtrace [])   ; => [\"file.rb:1:in `<main>`\"]
   ```"
  (:require [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable to-s ruby-class ruby-ancestors]]
            [sri.ruby-method-registry :refer [register-method]]))

;; =============================================================================
;; Core Exception Record
;; =============================================================================

(defrecord RubyException [message backtrace cause exception-class]
  RubyObject
  (ruby-class [_] exception-class)
  (ruby-ancestors [_]
    (case exception-class
      "Exception" ["Exception", "Object", "Kernel", "BasicObject"]
      "StandardError" ["StandardError", "Exception", "Object", "Kernel", "BasicObject"]
      "RuntimeError" ["RuntimeError", "StandardError", "Exception", "Object", "Kernel", "BasicObject"]
      "NoMethodError" ["NoMethodError", "NameError", "StandardError", "Exception", "Object", "Kernel", "BasicObject"]
      "ArgumentError" ["ArgumentError", "StandardError", "Exception", "Object", "Kernel", "BasicObject"]
      "LocalJumpError" ["LocalJumpError", "StandardError", "Exception", "Object", "Kernel", "BasicObject"]
      "NameError" ["NameError", "StandardError", "Exception", "Object", "Kernel", "BasicObject"]
      "TypeError" ["TypeError", "StandardError", "Exception", "Object", "Kernel", "BasicObject"]
      "ScriptError" ["ScriptError", "Exception", "Object", "Kernel", "BasicObject"]
      "SyntaxError" ["SyntaxError", "ScriptError", "Exception", "Object", "Kernel", "BasicObject"]
      "LoadError" ["LoadError", "ScriptError", "Exception", "Object", "Kernel", "BasicObject"]
      "SystemError" ["SystemError", "Exception", "Object", "Kernel", "BasicObject"]
      "SignalException" ["SignalException", "Exception", "Object", "Kernel", "BasicObject"]
      ;; Default fallback
      ["Exception", "Object", "Kernel", "BasicObject"]))

  RubyInspectable
  (to-s [_]
    (str message))
  (inspect [this]
    (str "#<" exception-class ": " message ">")))

;; =============================================================================
;; Exception Factory Functions
;; =============================================================================

(defn create-exception
  "Create a generic Ruby Exception object."
  ([message] (create-exception message [] nil "Exception"))
  ([message backtrace] (create-exception message backtrace nil "Exception"))
  ([message backtrace cause] (create-exception message backtrace cause "Exception"))
  ([message backtrace cause exception-class]
   (->RubyException message (or backtrace []) cause exception-class)))

(defn create-standard-error
  "Create a StandardError (default rescue type)."
  ([message] (create-exception message [] nil "StandardError"))
  ([message backtrace] (create-exception message backtrace nil "StandardError"))
  ([message backtrace cause] (create-exception message backtrace cause "StandardError")))

(defn create-runtime-error
  "Create a RuntimeError (default for raise with string)."
  ([message] (create-exception message [] nil "RuntimeError"))
  ([message backtrace] (create-exception message backtrace nil "RuntimeError"))
  ([message backtrace cause] (create-exception message backtrace cause "RuntimeError")))

(defn create-no-method-error
  "Create a NoMethodError."
  ([message] (create-exception message [] nil "NoMethodError"))
  ([message backtrace] (create-exception message backtrace nil "NoMethodError"))
  ([message backtrace cause] (create-exception message backtrace cause "NoMethodError")))

(defn create-argument-error
  "Create an ArgumentError."
  ([message] (create-exception message [] nil "ArgumentError"))
  ([message backtrace] (create-exception message backtrace nil "ArgumentError"))
  ([message backtrace cause] (create-exception message backtrace cause "ArgumentError")))

(defn create-local-jump-error
  "Create a LocalJumpError (for invalid break/next/return)."
  ([message] (create-exception message [] nil "LocalJumpError"))
  ([message backtrace] (create-exception message backtrace nil "LocalJumpError"))
  ([message backtrace cause] (create-exception message backtrace cause "LocalJumpError")))

(defn create-name-error
  "Create a NameError (undefined variable/constant)."
  ([message] (create-exception message [] nil "NameError"))
  ([message backtrace] (create-exception message backtrace nil "NameError"))
  ([message backtrace cause] (create-exception message backtrace cause "NameError")))

(defn create-type-error
  "Create a TypeError."
  ([message] (create-exception message [] nil "TypeError"))
  ([message backtrace] (create-exception message backtrace nil "TypeError"))
  ([message backtrace cause] (create-exception message backtrace cause "TypeError")))

(defn create-syntax-error
  "Create a SyntaxError."
  ([message] (create-exception message [] nil "SyntaxError"))
  ([message backtrace] (create-exception message backtrace nil "SyntaxError"))
  ([message backtrace cause] (create-exception message backtrace cause "SyntaxError")))

;; =============================================================================
;; Exception Method Implementations
;; =============================================================================

(defn exception-message
  "Return the exception's message."
  [exception & args]
  (if (instance? RubyException exception)
    (:message exception)
    (throw (ex-info "message called on non-exception object" {:object exception}))))

(defn exception-backtrace
  "Return the exception's backtrace array."
  [exception & args]
  (if (instance? RubyException exception)
    (:backtrace exception)
    (throw (ex-info "backtrace called on non-exception object" {:object exception}))))

(defn exception-cause
  "Return the exception that caused this exception."
  [exception & args]
  (if (instance? RubyException exception)
    (:cause exception)
    (throw (ex-info "cause called on non-exception object" {:object exception}))))

(defn exception-full-message
  "Return a formatted error message with backtrace."
  [exception & args]
  (if (instance? RubyException exception)
    (let [msg (:message exception)
          bt (:backtrace exception)
          class-name (:exception-class exception)]
      (str class-name ": " msg
           (when (seq bt)
             (str "\n" (clojure.string/join "\n" bt)))))
    (throw (ex-info "full_message called on non-exception object" {:object exception}))))

(defn exception-inspect
  "Return a detailed string representation of the exception."
  [exception & args]
  (if (instance? RubyException exception)
    (str "#<" (:exception-class exception) ": " (:message exception) ">")
    (throw (ex-info "inspect called on non-exception object" {:object exception}))))

(defn exception-to-s
  "Return the exception message as a string."
  [exception & args]
  (if (instance? RubyException exception)
    (:message exception)
    (throw (ex-info "to_s called on non-exception object" {:object exception}))))

;; =============================================================================
;; Utility Functions
;; =============================================================================

(defn ruby-exception?
  "Check if an object is a Ruby exception."
  [obj]
  (instance? RubyException obj))

(defn standard-error?
  "Check if an exception is a StandardError or its subclass."
  [exception]
  (when (ruby-exception? exception)
    (let [ancestors (ruby-ancestors exception)]
      (some #{"StandardError"} ancestors))))

(defn exception-inherits-from?
  "Check if an exception inherits from a given class name."
  [exception class-name]
  (when (ruby-exception? exception)
    (let [ancestors (ruby-ancestors exception)]
      (some #{class-name} ancestors))))

(defn invoke-exception-method
  "Invoke a method on a Ruby exception object."
  [exception method-name args]
  (case method-name
    :message (exception-message exception args)
    :backtrace (exception-backtrace exception args)
    :cause (exception-cause exception args)
    :full_message (exception-full-message exception args)
    :inspect (exception-inspect exception args)
    :to_s (exception-to-s exception args)
    (throw (ex-info (str "Undefined method: " method-name " for exception")
                   {:exception exception :method method-name}))))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-exception-methods!
  "Register all exception methods in the method registry."
  []
  ;; Exception instance methods
  (register-method "Exception" :message exception-message)
  (register-method "Exception" :backtrace exception-backtrace)
  (register-method "Exception" :cause exception-cause)
  (register-method "Exception" :full_message exception-full-message)
  (register-method "Exception" :inspect exception-inspect)
  (register-method "Exception" :to_s exception-to-s)

  ;; All exception subclasses inherit these methods automatically
  ;; through the ancestor chain lookup in ruby-method-registry
  )

;; Initialize exception methods on namespace load
(register-exception-methods!)

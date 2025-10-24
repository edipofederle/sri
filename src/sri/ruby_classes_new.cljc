(ns sri.ruby-classes-new
  "Ruby class hierarchy implementation - refactored with separate files for each type.
   
   This is the main interface that brings together all Ruby classes and provides
   the same API as the original ruby-classes namespace."
  (:require [sri.ruby-protocols :as protocols]
            [sri.ruby-method-registry :as registry]
            [sri.ruby-basic-object :as basic-obj]
            [sri.ruby-object :as ruby-obj]
            [sri.ruby-string :as ruby-str]
            [sri.ruby-range :as ruby-range]
            [sri.ruby-array :as ruby-array]
            [sri.ruby-numeric :as ruby-numeric]))

;; =============================================================================
;; Re-export Constructor Functions
;; =============================================================================

;; BasicObject
(def create-basic-object basic-obj/create-basic-object)

;; Object 
(def create-object ruby-obj/create-object)
(def ruby-nil ruby-obj/ruby-nil)
(def ruby-true ruby-obj/ruby-true)
(def ruby-false ruby-obj/ruby-false)

;; String
(def create-string ruby-str/create-string)

;; Range
(def create-range ruby-range/create-range)
(def ruby-range? ruby-range/ruby-range?)
(def char-range? ruby-range/char-range?)
(def char-to-int ruby-range/char-to-int)
(def int-to-char ruby-range/int-to-char)

;; Array
(def create-array ruby-array/create-array)
(def create-empty-array ruby-array/create-empty-array)
(def vector->ruby-array ruby-array/vector->ruby-array)
(def ruby-array? ruby-array/ruby-array?)
(def mutable-array? ruby-array/mutable-array?)
(def array-get ruby-array/array-get)
(def array-set! ruby-array/array-set!)
(def array-push! ruby-array/array-push!)
(def array-pop! ruby-array/array-pop!)

;; Numeric
(def ruby-numeric? ruby-numeric/ruby-numeric?)
(def ruby-integer? ruby-numeric/ruby-integer?)
(def ruby-float? ruby-numeric/ruby-float?)
(def numeric-ruby-class ruby-numeric/numeric-ruby-class)
(def numeric-ruby-ancestors ruby-numeric/numeric-ruby-ancestors)

;; =============================================================================
;; Re-export Record Constructors
;; =============================================================================

(def ->BasicObject basic-obj/->BasicObject)
(def ->RubyObjectClass ruby-obj/->RubyObjectClass)
(def ->RubyString ruby-str/->RubyString)
(def ->RubyRange ruby-range/->RubyRange)
(def ->RubyArray ruby-array/->RubyArray)

;; =============================================================================
;; Method Call Interface
;; =============================================================================

(defn invoke-ruby-method
  "Main interface for calling Ruby methods on objects."
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

(def ruby-class protocols/ruby-class)
(def ruby-ancestors protocols/ruby-ancestors)
(def respond-to? protocols/respond-to?)
(def to-s protocols/to-s)
(def inspect protocols/inspect)
(def ruby-eq protocols/ruby-eq)
(def ruby-compare protocols/ruby-compare)

;; =============================================================================
;; Re-export Protocols  
;; =============================================================================

(def RubyObject protocols/RubyObject)
(def RubyInspectable protocols/RubyInspectable)
(def RubyComparable protocols/RubyComparable)

;; =============================================================================
;; Re-export Method Registry Functions
;; =============================================================================

(def has-method? registry/has-method?)
(def method-lookup registry/method-lookup)
(def call-ruby-method registry/call-ruby-method)
(def register-method registry/register-method)
(def get-ruby-method-impl registry/get-ruby-method-impl)

;; =============================================================================
;; Re-export Debugging and Introspection
;; =============================================================================

(def debug-method-registry registry/debug-method-registry)
(def class-methods registry/class-methods)
(def all-ruby-classes registry/all-ruby-classes)
(ns sri.ruby-numeric
  "Ruby Numeric class hierarchy implementation (Numeric, Integer, Float)."
  (:require [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to?
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn ruby-numeric?
  "Check if a value is a Ruby numeric (including Java numbers for now)."
  [value]
  (number? value))

(defn ruby-integer?
  "Check if a value is a Ruby integer."
  [value]
  (integer? value))

(defn ruby-float? 
  "Check if a value is a Ruby float."
  [value]
  (float? value))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-numeric-methods!
  "Register all Numeric methods in the method registry."
  []
  ;; Numeric methods (for all numbers)
  (register-method "Numeric" :negative? #(< % 0))
  (register-method "Numeric" :positive? #(> % 0))
  (register-method "Numeric" :zero? #(= % 0))
  (register-method "Numeric" :real? (constantly true))
  (register-method "Numeric" :integer? #(integer? %))
  (register-method "Numeric" :finite? #(and (number? %) (not (Double/isInfinite %)) (not (Double/isNaN %))))
  (register-method "Numeric" :infinite? #(and (number? %) (Double/isInfinite %)))
  (register-method "Numeric" :nan? #(and (number? %) (Double/isNaN %)))
  (register-method "Numeric" :abs #(Math/abs %))
  (register-method "Numeric" :round #(Math/round %))
  (register-method "Numeric" :ceil #(Math/ceil %))
  (register-method "Numeric" :floor #(Math/floor %))
  
  ;; Comparison operators
  (register-method "Numeric" :== #(= %1 %2))
  (register-method "Numeric" :!= #(not= %1 %2))
  (register-method "Numeric" :< #(< %1 %2))
  (register-method "Numeric" :> #(> %1 %2))
  (register-method "Numeric" :<= #(<= %1 %2))
  (register-method "Numeric" :>= #(>= %1 %2))
  (register-method "Numeric" "<=>" #(compare %1 %2))
  
  ;; Arithmetic operators
  (register-method "Numeric" :+ #(+ %1 %2))
  (register-method "Numeric" :- #(- %1 %2))
  (register-method "Numeric" :* #(* %1 %2))
  (register-method "Numeric" :/ #(/ %1 %2))
  (register-method "Numeric" :% #(mod %1 %2))
  (register-method "Numeric" "**" #(Math/pow %1 %2))
  
  ;; Conversion methods
  (register-method "Numeric" :to_i #(int %))
  (register-method "Numeric" :to_f #(float %))
  (register-method "Numeric" :to_s #(sri.ruby-string/->RubyString (str %)))
  (register-method "Numeric" :inspect #(str %)))

(defn register-integer-methods!
  "Register all Integer methods in the method registry."
  []
  ;; Inherit all Numeric methods first
  (register-numeric-methods!)
  
  ;; Integer-specific methods
  (register-method "Integer" :even? #(even? %))
  (register-method "Integer" :odd? #(odd? %))
  (register-method "Integer" :next #(inc %))
  (register-method "Integer" :succ #(inc %))
  (register-method "Integer" :pred #(dec %))
  
  ;; Block iteration methods
  (register-method "Integer" :times
    (fn [n block-fn]
      (dotimes [i n]
        (when block-fn
          (block-fn i)))
      n))  ; Return the original number
  
  (register-method "Integer" :upto
    (fn [from to block-fn]
      (doseq [i (range from (inc to))]
        (when block-fn
          (block-fn i)))
      from))
  
  (register-method "Integer" :downto
    (fn [from to block-fn]
      (doseq [i (range from (dec to) -1)]
        (when block-fn
          (block-fn i)))
      from))
  
  ;; Bitwise operations  
  (register-method "Integer" "&" (fn [a b] (bit-and a b)))
  (register-method "Integer" "|" (fn [a b] (bit-or a b)))
  (register-method "Integer" "^" (fn [a b] (bit-xor a b)))
  (register-method "Integer" "~" (fn [a] (bit-not a)))
  (register-method "Integer" "<<" (fn [a b] (bit-shift-left a b)))
  (register-method "Integer" ">>" (fn [a b] (bit-shift-right a b))))

(defn register-float-methods!
  "Register all Float methods in the method registry."
  []
  ;; Inherit all Numeric methods first
  (register-numeric-methods!)
  
  ;; Float-specific methods
  (register-method "Float" :nan? #(Double/isNaN %))
  (register-method "Float" :infinite? #(Double/isInfinite %))
  (register-method "Float" :finite? #(Double/isFinite %)))

;; Register methods on namespace load
(register-integer-methods!)
(register-float-methods!)

;; =============================================================================
;; Ruby Class Name Resolution
;; =============================================================================

(defn numeric-ruby-class
  "Get the Ruby class name for a numeric value."
  [value]
  (cond
    (integer? value) "Integer"
    (float? value) "Float"
    (ratio? value) "Rational"
    (number? value) "Numeric"
    :else nil))

(defn numeric-ruby-ancestors
  "Get the Ruby ancestor chain for a numeric value."
  [value]
  (cond
    (integer? value) ["Integer" "Numeric" "Object" "Kernel" "BasicObject"]
    (float? value) ["Float" "Numeric" "Object" "Kernel" "BasicObject"]
    (ratio? value) ["Rational" "Numeric" "Object" "Kernel" "BasicObject"]
    (number? value) ["Numeric" "Object" "Kernel" "BasicObject"]
    :else []))
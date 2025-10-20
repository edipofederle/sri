(ns sri.ruby-rational
  "Ruby Rational class implementation."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to? get-ruby-method
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]))

;; =============================================================================
;; Rational Number Implementation
;; =============================================================================

(defn gcd
  "Calculate greatest common divisor for any numeric type."
  [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))

(defn abs-value
  "Get absolute value for any numeric type."
  [n]
  (if (< n 0) (- n) n))

(defn simplify-rational
  "Simplify a rational number by reducing to lowest terms."
  [numerator denominator]
  (let [gcd-val (gcd (abs-value numerator) (abs-value denominator))
        num (/ numerator gcd-val)
        den (/ denominator gcd-val)]
    ;; Handle negative denominators
    (if (< den 0)
      [(- num) (- den)]
      [num den])))

(defrecord RubyRational [numerator denominator]
  RubyObject
  (ruby-class [_] "Rational")
  (ruby-ancestors [_] ["Rational" "Numeric" "Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :numerator :denominator :to_f :to_i :to_r
                 :+ :- :* :/ :== :!= :< :> :<= :>= :<=> "+@" "-@"
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :class :nil?} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))
  
  Object
  (toString [this]
    (str "Rational(" (:numerator this) ", " (:denominator this) ")"))

  RubyInspectable
  (to-s [this]
    (if (= (:denominator this) 1)
      (str (:numerator this))
      (str (:numerator this) "/" (:denominator this))))
  (inspect [this]
    (str "(" (:numerator this) "/" (:denominator this) ")"))

  RubyComparable
  (ruby-eq [this other]
    (cond
      (instance? RubyRational other)
      (and (= (:numerator this) (:numerator other))
           (= (:denominator this) (:denominator other)))

      (number? other)
      (and (= (:denominator this) 1)
           (= (:numerator this) other))

      :else false))

  (ruby-compare [this other]
    (cond
      (instance? RubyRational other)
      (let [left-val (/ (:numerator this) (:denominator this))
            right-val (/ (:numerator other) (:denominator other))]
        (compare left-val right-val))

      (number? other)
      (let [this-val (/ (:numerator this) (:denominator this))]
        (compare this-val other))

      :else 0)))

;; =============================================================================
;; Constructor Functions
;; =============================================================================

(defn ->RubyRational
  "Create a new RubyRational with automatic simplification."
  ([numerator] (->RubyRational numerator 1))
  ([numerator denominator]
   (when (zero? denominator)
     (throw (ex-info "divided by 0" {:numerator numerator :denominator denominator})))
   (let [[num den] (simplify-rational numerator denominator)]
     (RubyRational. num den))))

(defn ruby-rational?
  "Check if a value is a RubyRational."
  [value]
  (instance? RubyRational value))

;; =============================================================================
;; Method Implementations
;; =============================================================================

(defn rational-add [this other]
  (cond
    (ruby-rational? other)
    (let [num (+ (* (:numerator this) (:denominator other))
                 (* (:numerator other) (:denominator this)))
          den (* (:denominator this) (:denominator other))]
      (->RubyRational num den))

    (number? other)
    (rational-add this (->RubyRational other 1))

    :else (throw (ex-info "Invalid addition operand" {:other other}))))

(defn rational-subtract [this other]
  (cond
    (ruby-rational? other)
    (let [num (- (* (:numerator this) (:denominator other))
                 (* (:numerator other) (:denominator this)))
          den (* (:denominator this) (:denominator other))]
      (->RubyRational num den))

    (number? other)
    (rational-subtract this (->RubyRational other 1))

    :else (throw (ex-info "Invalid subtraction operand" {:other other}))))

(defn rational-multiply [this other]
  (cond
    (ruby-rational? other)
    (->RubyRational (* (:numerator this) (:numerator other))
                    (* (:denominator this) (:denominator other)))

    (number? other)
    (->RubyRational (* (:numerator this) other) (:denominator this))

    :else (throw (ex-info "Invalid multiplication operand" {:other other}))))

(defn rational-divide [this other]
  (cond
    (ruby-rational? other)
    (do
      (when (zero? (:numerator other))
        (throw (ex-info "divided by 0" {:other other})))
      (->RubyRational (* (:numerator this) (:denominator other))
                      (* (:denominator this) (:numerator other))))

    (number? other)
    (do
      (when (zero? other)
        (throw (ex-info "divided by 0" {:other other})))
      (->RubyRational (:numerator this) (* (:denominator this) other)))

    :else (throw (ex-info "Invalid division operand" {:other other}))))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-rational-methods!
  "Register all Rational methods in the method registry."
  []
  ;; Inherit from Object
  (register-method "Rational" :to_s #(to-s %))
  (register-method "Rational" :inspect #(inspect %))
  (register-method "Rational" :== #(ruby-eq %1 %2))
  (register-method "Rational" :!= #(not (ruby-eq %1 %2)))
  (register-method "Rational" :equal? #(identical? %1 %2))
  (register-method "Rational" :object_id #(System/identityHashCode %))
  (register-method "Rational" :class #(ruby-class %))
  (register-method "Rational" :nil? #(false)) ; Rationals are never nil
  (register-method "Rational" :respond_to? #(respond-to? %1 %2))
  (register-method "Rational" :methods #(class-methods (ruby-class %)))
  (register-method "Rational" :instance_of? #(= (ruby-class %1) %2))
  (register-method "Rational" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
  (register-method "Rational" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

  ;; Rational-specific methods
  (register-method "Rational" :numerator #(:numerator %))
  (register-method "Rational" :denominator #(:denominator %))
  (register-method "Rational" :to_f #(double (/ (:numerator %) (:denominator %))))
  (register-method "Rational" :to_i #(int (/ (:numerator %) (:denominator %))))
  (register-method "Rational" :to_r #(%))

  ;; Arithmetic operations
  (register-method "Rational" :+ #(rational-add %1 %2))
  (register-method "Rational" :- #(rational-subtract %1 %2))
  (register-method "Rational" :* #(rational-multiply %1 %2))
  (register-method "Rational" :/ #(rational-divide %1 %2))

  ;; Unary operations
  (register-method "Rational" "-@" #(->RubyRational (- (:numerator %)) (:denominator %)))
  (register-method "Rational" "+@" #(%))

  ;; Comparison operations
  (register-method "Rational" :<=> #(ruby-compare %1 %2))
  (register-method "Rational" :< #(< (ruby-compare %1 %2) 0))
  (register-method "Rational" :> #(> (ruby-compare %1 %2) 0))
  (register-method "Rational" :<= #(<= (ruby-compare %1 %2) 0))
  (register-method "Rational" :>= #(>= (ruby-compare %1 %2) 0)))

;; Register methods on namespace load
(register-rational-methods!)

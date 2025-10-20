(ns sri.ruby-complex
  "Ruby Complex class implementation."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to? get-ruby-method
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]))

;; =============================================================================
;; Complex Number Implementation
;; =============================================================================

(defrecord RubyComplex [real imaginary]
  RubyObject
  (ruby-class [_] "Complex")
  (ruby-ancestors [_] ["Complex" "Numeric" "Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :real :imaginary :to_f :to_i :to_c
                 :+ :- :* :/ :== :!= :< :> :<= :>= :<=> "+@" "-@"
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :class :nil?} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))
  
  Object
  (toString [this]
    (str "Complex(" (:real this) ", " (:imaginary this) ")"))

  RubyInspectable
  (to-s [this]
    (cond
      (zero? (:real this)) (str (:imaginary this) "i")
      (zero? (:imaginary this)) (str (:real this))
      (> (:imaginary this) 0) (str (:real this) "+" (:imaginary this) "i")
      :else (str (:real this) (:imaginary this) "i")))
  (inspect [this]
    (str "(" (:real this) (if (>= (:imaginary this) 0) "+" "") (:imaginary this) "i)"))

  RubyComparable
  (ruby-eq [this other]
    (cond
      (instance? RubyComplex other)
      (and (= (:real this) (:real other))
           (= (:imaginary this) (:imaginary other)))

      (number? other)
      (and (= (:real this) other)
           (zero? (:imaginary this)))

      :else false))

  (ruby-compare [this other]
    (cond
      (instance? RubyComplex other)
      ;; Complex numbers don't have a natural ordering, but we can compare magnitudes
      (let [this-mag (+ (* (:real this) (:real this)) (* (:imaginary this) (:imaginary this)))
            other-mag (+ (* (:real other) (:real other)) (* (:imaginary other) (:imaginary other)))]
        (compare this-mag other-mag))

      (number? other)
      (if (zero? (:imaginary this))
        (compare (:real this) other)
        ;; Complex with imaginary part is "greater" than real number
        1)

      :else 0)))

;; =============================================================================
;; Constructor Functions
;; =============================================================================

(defn ->RubyComplex
  "Create a new RubyComplex."
  ([imaginary] (->RubyComplex 0 imaginary))
  ([real imaginary]
   (RubyComplex. real imaginary)))

(defn ruby-complex?
  "Check if a value is a RubyComplex."
  [value]
  (instance? RubyComplex value))

;; =============================================================================
;; Method Implementations
;; =============================================================================

(defn complex-add [this other]
  (cond
    (ruby-complex? other)
    (->RubyComplex (+ (:real this) (:real other))
                   (+ (:imaginary this) (:imaginary other)))

    (number? other)
    (->RubyComplex (+ (:real this) other) (:imaginary this))

    :else (throw (ex-info "Invalid addition operand" {:other other}))))

(defn complex-subtract [this other]
  (cond
    (ruby-complex? other)
    (->RubyComplex (- (:real this) (:real other))
                   (- (:imaginary this) (:imaginary other)))

    (number? other)
    (->RubyComplex (- (:real this) other) (:imaginary this))

    :else (throw (ex-info "Invalid subtraction operand" {:other other}))))

(defn complex-multiply [this other]
  (cond
    (ruby-complex? other)
    ;; (a + bi)(c + di) = (ac - bd) + (ad + bc)i
    (let [a (:real this)
          b (:imaginary this)
          c (:real other)
          d (:imaginary other)]
      (->RubyComplex (- (* a c) (* b d))
                     (+ (* a d) (* b c))))

    (number? other)
    (->RubyComplex (* (:real this) other) (* (:imaginary this) other))

    :else (throw (ex-info "Invalid multiplication operand" {:other other}))))

(defn complex-divide [this other]
  (cond
    (ruby-complex? other)
    ;; (a + bi)/(c + di) = ((ac + bd) + (bc - ad)i)/(c² + d²)
    (let [a (:real this)
          b (:imaginary this)
          c (:real other)
          d (:imaginary other)
          denominator (+ (* c c) (* d d))]
      (when (zero? denominator)
        (throw (ex-info "divided by 0" {:other other})))
      (->RubyComplex (/ (+ (* a c) (* b d)) denominator)
                     (/ (- (* b c) (* a d)) denominator)))

    (number? other)
    (do
      (when (zero? other)
        (throw (ex-info "divided by 0" {:other other})))
      (->RubyComplex (/ (:real this) other) (/ (:imaginary this) other)))

    :else (throw (ex-info "Invalid division operand" {:other other}))))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-complex-methods!
  "Register all Complex methods in the method registry."
  []
  ;; Inherit from Object
  (register-method "Complex" :to_s #(to-s %))
  (register-method "Complex" :inspect #(inspect %))
  (register-method "Complex" :== #(ruby-eq %1 %2))
  (register-method "Complex" :!= #(not (ruby-eq %1 %2)))
  (register-method "Complex" :equal? #(identical? %1 %2))
  (register-method "Complex" :object_id #(System/identityHashCode %))
  (register-method "Complex" :class #(ruby-class %))
  (register-method "Complex" :nil? #(false)) ; Complex numbers are never nil
  (register-method "Complex" :respond_to? #(respond-to? %1 %2))
  (register-method "Complex" :methods #(class-methods (ruby-class %)))
  (register-method "Complex" :instance_of? #(= (ruby-class %1) %2))
  (register-method "Complex" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
  (register-method "Complex" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

  ;; Complex-specific methods
  (register-method "Complex" :real #(:real %))
  (register-method "Complex" :imaginary #(:imaginary %))
  (register-method "Complex" :to_f #(if (zero? (:imaginary %)) 
                                      (double (:real %))
                                      (throw (ex-info "can't convert Complex to Float" {}))))
  (register-method "Complex" :to_i #(if (zero? (:imaginary %)) 
                                      (int (:real %))
                                      (throw (ex-info "can't convert Complex to Integer" {}))))
  (register-method "Complex" :to_c #(%))

  ;; Arithmetic operations
  (register-method "Complex" :+ #(complex-add %1 %2))
  (register-method "Complex" :- #(complex-subtract %1 %2))
  (register-method "Complex" :* #(complex-multiply %1 %2))
  (register-method "Complex" :/ #(complex-divide %1 %2))

  ;; Unary operations
  (register-method "Complex" "-@" #(->RubyComplex (- (:real %)) (- (:imaginary %))))
  (register-method "Complex" "+@" #(%))

  ;; Comparison operations (limited for complex numbers)
  (register-method "Complex" :<=> #(ruby-compare %1 %2)))

;; Register methods on namespace load
(register-complex-methods!)
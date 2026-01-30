(ns sri.ruby-object
  "Ruby Object class - main Ruby object class that includes Kernel."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to?
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]
            [sri.ruby-kernel :as kernel]))

(defrecord RubyObjectClass [value]
  RubyObject
  (ruby-class [_] "Object")
  (ruby-ancestors [_] ["Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :class :nil? :== :!= :puts :p :print
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :should} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))

  RubyInspectable
  (to-s [this] (str (:value this)))
  (inspect [this]
    (let [val (:value this)]
      (if (nil? val)
        "nil"
        (str "#<Object:0x" (Integer/toHexString (System/identityHashCode this))
             " @value=" (pr-str val) ">"))))

  RubyComparable
  (ruby-eq [this other]
    (if (instance? RubyObjectClass other)
      (= (:value this) (:value other))
      false))
  (ruby-compare [this other]
    (when (instance? RubyObjectClass other)
      (compare (:value this) (:value other)))))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-object-methods!
  "Register all Object methods in the method registry."
  []
  ;; Inherit from BasicObject
  (register-method "Object" :to_s #(to-s %))
  (register-method "Object" :inspect #(inspect %))
  (register-method "Object" :== #(ruby-eq %1 %2))
  (register-method "Object" :!= #(not (ruby-eq %1 %2)))
  (register-method "Object" :equal? #(identical? %1 %2))
  (register-method "Object" :object_id #(System/identityHashCode %))

  ;; Object-specific methods
  ;; Return a class object (map with :name) so that .class.name works
  (register-method "Object" :class #(let [class-name (ruby-class %)]
                                       {:name class-name :builtin true}))
  (register-method "Object" :nil? #(nil? (:value %)))
  (register-method "Object" :respond_to? #(respond-to? %1 %2))
  (register-method "Object" :methods #(class-methods (ruby-class %)))
  (register-method "Object" :instance_of? #(= (ruby-class %1) %2))
  (register-method "Object" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
  (register-method "Object" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

  ;; Include Kernel methods (mixed into Object)
  (kernel/register-kernel-methods!))

;; Register methods on namespace load
(register-object-methods!)

;; =============================================================================
;; Constructor Functions
;; =============================================================================

(defn create-object
  "Create a new Object instance with optional value."
  ([] (->RubyObjectClass nil))
  ([value] (->RubyObjectClass value)))

(defn ruby-nil
  "Create Ruby nil object."
  []
  (->RubyObjectClass nil))

(defn ruby-true
  "Create Ruby true object."
  []
  (->RubyObjectClass true))

(defn ruby-false
  "Create Ruby false object."
  []
  (->RubyObjectClass false))

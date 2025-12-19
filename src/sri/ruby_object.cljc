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
  (register-method "Object" :class #(ruby-class %))
  (register-method "Object" :nil? #(nil? (:value %)))
  (register-method "Object" :respond_to? #(respond-to? %1 %2))
  (register-method "Object" :methods #(class-methods (ruby-class %)))
  (register-method "Object" :instance_of? #(= (ruby-class %1) %2))
  (register-method "Object" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
  (register-method "Object" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

  ;; Include Kernel methods (mixed into Object)
  (kernel/register-kernel-methods!)

  ;; Spec assertion method
  (register-method "Object" :should
    (fn [obj]
      ;; Return a spec expectation object that handles assertions
      (reify RubyObject
        (ruby-class [_] "Spec::Expectations::ObjectExpectation")
        (ruby-ancestors [_] ["Spec::Expectations::ObjectExpectation" "Object" "Kernel" "BasicObject"])
        (respond-to? [_ method-name]
          (contains? #{:== :!= :be_nil :be_true :be_false :be_kind_of} method-name))
        (get-ruby-method [this method-name]
          (case method-name
            :== (fn [_ expected]
                  (let [result (if (satisfies? RubyObject obj)
                                 (ruby-eq obj expected) 
                                 (= obj expected))]
                    (when-not result
                      (throw (ex-info (str "Expected " (pr-str expected) " but got " (pr-str obj))
                                      {:type :assertion-failure
                                       :expected expected
                                       :actual obj})))
                    result))
            :!= (fn [_ expected]
                  (let [result (if (satisfies? RubyObject obj)
                                 (not (ruby-eq obj expected))
                                 (not= obj expected))]
                    (when-not result
                      (throw (ex-info (str "Expected not to equal " (pr-str expected) " but got " (pr-str obj))
                                      {:type :assertion-failure
                                       :expected-not expected
                                       :actual obj})))
                    result))
            :be_nil (fn [_]
                      (let [result (nil? obj)]
                        (when-not result
                          (throw (ex-info (str "Expected nil but got " (pr-str obj))
                                          {:type :assertion-failure
                                           :expected nil
                                           :actual obj})))
                        result))
            :be_true (fn [_]
                       (let [result (= obj true)]
                         (when-not result
                           (throw (ex-info (str "Expected true but got " (pr-str obj))
                                           {:type :assertion-failure
                                            :expected true
                                            :actual obj})))
                         result))
            :be_false (fn [_]
                        (let [result (= obj false)]
                          (when-not result
                            (throw (ex-info (str "Expected false but got " (pr-str obj))
                                            {:type :assertion-failure
                                             :expected false
                                             :actual obj})))
                          result))
            :be_kind_of (fn [_ expected-class]
                          (let [obj-class (if (satisfies? RubyObject obj)
                                           (ruby-class obj)
                                           (cond
                                             (string? obj) "String"
                                             (integer? obj) "Integer"
                                             (vector? obj) "Array"
                                             :else "Object"))
                                result (= obj-class expected-class)]
                            (when-not result
                              (throw (ex-info (str "Expected " expected-class " but got " obj-class)
                                              {:type :assertion-failure
                                               :expected expected-class
                                               :actual obj-class})))
                            result))
            nil))
        
        RubyComparable
        (ruby-eq [this expected]
          (if (satisfies? RubyObject obj)
            (ruby-eq obj expected) 
            (= obj expected)))))))

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

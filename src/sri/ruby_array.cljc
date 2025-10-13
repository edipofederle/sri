(ns sri.ruby-array
  "Ruby Array class implementation with proper mutability."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to?
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]))

;; =============================================================================
;; Mutable Array Wrapper
;; =============================================================================

(defrecord RubyArray [data]  ; data is an atom containing a vector
  RubyObject
  (ruby-class [_] "Array")
  (ruby-ancestors [_] ["Array" "Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :length :size "[]" "[]=" :push :<< :pop :first :last
                 :each :map :select :empty? :== :!= :+ 
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :class :nil? :puts :p :print} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))
  
  RubyInspectable
  (to-s [this] 
    (let [elements @(:data this)]
      (str "[" (str/join " " (map #(cond
                                     (nil? %) "nil"
                                     (keyword? %) (name %) ; symbols without :
                                     (string? %) (str "\"" % "\"") ; strings with quotes
                                     (satisfies? RubyInspectable %) (to-s %)
                                     :else (str %)) elements)) "]")))
  (inspect [this] (to-s this))
  
  RubyComparable
  (ruby-eq [this other]
    (and (instance? RubyArray other)
         (= @(:data this) @(:data other))))
  (ruby-compare [this other]
    (when (instance? RubyArray other)
      (compare @(:data this) @(:data other)))))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn ruby-array?
  "Check if a value is a Ruby array."
  [value]
  (instance? RubyArray value))

(defn mutable-array?
  "Check if a value is a mutable array (for compatibility)."
  [value]
  (instance? RubyArray value))

(defn array-get
  "Get element at index from Ruby array."
  [ruby-array index]
  (let [elements @(:data ruby-array)
        len (count elements)
        idx (if (< index 0) (+ len index) index)]
    (if (and (>= idx 0) (< idx len))
      (nth elements idx)
      nil)))

(defn array-set!
  "Set element at index in Ruby array (mutating operation)."
  [ruby-array index value]
  (let [current-elements @(:data ruby-array)
        len (count current-elements)
        idx (if (< index 0) (+ len index) index)]
    (if (>= idx 0)
      (let [;; Expand array if needed
            expanded (if (>= idx len)
                      (vec (concat current-elements (repeat (- idx len -1) nil)))
                      current-elements)
            updated (assoc expanded idx value)]
        (reset! (:data ruby-array) updated)
        value)
      (throw (ex-info "Negative index too large" {:index index :array-size len})))))

(defn array-push!
  "Push element to end of array (mutating operation)."
  [ruby-array value]
  (swap! (:data ruby-array) conj value)
  ruby-array)

(defn array-pop!
  "Remove and return last element (mutating operation)."
  [ruby-array]
  (let [current @(:data ruby-array)]
    (if (empty? current)
      nil
      (let [last-elem (last current)
            new-vec (vec (butlast current))]
        (reset! (:data ruby-array) new-vec)
        last-elem))))

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-array-methods!
  "Register all Array methods in the method registry."
  []
  ;; Inherit from Object
  (register-method "Array" :to_s #(to-s %))
  (register-method "Array" :inspect #(inspect %))
  (register-method "Array" :== #(ruby-eq %1 %2))
  (register-method "Array" :!= #(not (ruby-eq %1 %2)))
  (register-method "Array" :equal? #(identical? %1 %2))
  (register-method "Array" :object_id #(System/identityHashCode %))
  (register-method "Array" :class #(ruby-class %))
  (register-method "Array" :nil? #(false)) ; Arrays are never nil
  (register-method "Array" :respond_to? #(respond-to? %1 %2))
  (register-method "Array" :methods #(class-methods (ruby-class %)))
  (register-method "Array" :instance_of? #(= (ruby-class %1) %2))
  (register-method "Array" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
  (register-method "Array" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

  ;; Array-specific methods
  (register-method "Array" :length #(count @(:data %)))
  (register-method "Array" :size #(count @(:data %))) ; alias for length

  (register-method "Array" "[]"
    (fn [ruby-array index]
      (array-get ruby-array index)))

  (register-method "Array" "[]="
    (fn [ruby-array index value]
      (array-set! ruby-array index value)))

  (register-method "Array" :push
    (fn [ruby-array & values]
      (doseq [value values]
        (array-push! ruby-array value))
      ruby-array))

  (register-method "Array" :<<
    (fn [ruby-array value]
      (array-push! ruby-array value)))

  (register-method "Array" :pop
    (fn [ruby-array]
      (array-pop! ruby-array)))

  (register-method "Array" :first
    (fn [ruby-array]
      (array-get ruby-array 0)))

  (register-method "Array" :last
    (fn [ruby-array]
      (let [len (count @(:data ruby-array))]
        (if (> len 0)
          (array-get ruby-array (dec len))
          nil))))

  (register-method "Array" :empty?
    (fn [ruby-array]
      (empty? @(:data ruby-array))))

  ;; Array concatenation (returns new array)
  (register-method "Array" :+
    (fn [ruby-array1 ruby-array2]
      (cond
        (instance? RubyArray ruby-array2)
        (->RubyArray (atom (vec (concat @(:data ruby-array1) @(:data ruby-array2)))))
        
        (vector? ruby-array2)
        (->RubyArray (atom (vec (concat @(:data ruby-array1) ruby-array2))))
        
        :else
        (throw (ex-info "Array concatenation requires array argument" 
                       {:array1 ruby-array1 :array2 ruby-array2})))))

  ;; Inherit Kernel methods
  (register-method "Array" :puts 
    (fn [this & args]
      (if (empty? args)
        (println)
        (doseq [arg args]
          (println (if (satisfies? RubyInspectable arg) (to-s arg) (str arg)))))
      nil))

  (register-method "Array" :p 
    (fn [this & args]
      (if (empty? args)
        nil
        (let [results (mapv #(if (satisfies? RubyInspectable %) (inspect %) (pr-str %)) args)]
          (println (str/join " " results))
          (if (= 1 (count results)) (first args) (vec args))))))

  (register-method "Array" :print 
    (fn [this & args]
      (doseq [arg args]
        (print (if (satisfies? RubyInspectable arg) (to-s arg) (str arg))))
      nil)))

;; Register methods on namespace load
(register-array-methods!)

;; =============================================================================
;; Constructor Functions
;; =============================================================================

(defn create-array
  "Create a new Ruby array from elements."
  [& elements]
  (->RubyArray (atom (vec elements))))

(defn create-empty-array
  "Create a new empty Ruby array."
  []
  (->RubyArray (atom [])))

(defn vector->ruby-array
  "Convert a Clojure vector to a Ruby array."
  [vec]
  (->RubyArray (atom vec)))
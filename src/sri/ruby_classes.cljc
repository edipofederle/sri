(ns sri.ruby-classes
  "Ruby class hierarchy implementation using Clojure protocols and records.

   This module implements Ruby's object-oriented model:
   BasicObject → Kernel → Object → String/Integer/Array/etc."
  (:require [clojure.string :as str]))

;; =============================================================================
;; Core Protocols
;; =============================================================================

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

;; =============================================================================
;; Method Registry and Lookup
;; =============================================================================

(def ^:private method-registry
  "Registry of methods for each Ruby class."
  (atom {}))

(defn register-method
  "Register a method implementation for a Ruby class."
  [class-name method-name method-fn]
  (swap! method-registry
         assoc-in [class-name method-name] method-fn))

(defn has-method?
  "Check if a class has a specific method."
  [class-name method-name]
  (contains? (get @method-registry class-name {}) method-name))

(defn get-ruby-method-impl
  "Get method implementation for a class."
  [class-name method-name]
  (get-in @method-registry [class-name method-name]))

(defn method-lookup
  "Look up method in ancestor chain following Ruby's method resolution order."
  [obj method-name]
  (let [class-chain (ruby-ancestors obj)]
    (loop [chain class-chain]
      (when (seq chain)
        (let [current-class (first chain)]
          (if (has-method? current-class method-name)
            (get-ruby-method-impl current-class method-name)
            (recur (rest chain))))))))

(defn call-ruby-method
  "Call a method on a Ruby object with proper method lookup."
  [obj method-name & args]
  (if-let [method-fn (method-lookup obj method-name)]
    (apply method-fn obj args)
    (throw (ex-info (str "NoMethodError: undefined method `" method-name "` for " (ruby-class obj))
                    {:object obj :method method-name :args args}))))

;; =============================================================================
;; BasicObject - Root of Ruby class hierarchy
;; =============================================================================

(defrecord BasicObject []
  RubyObject
  (ruby-class [_] "BasicObject")
  (ruby-ancestors [_] ["BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :== :!= :equal? :object_id} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))

  RubyInspectable
  (to-s [_] "#<BasicObject>")
  (inspect [this] (to-s this))

  RubyComparable
  (ruby-eq [this other]
    (identical? this other))
  (ruby-compare [this other]
    (cond
      (identical? this other) 0
      :else nil))) ; BasicObject doesn't implement <=> by default

;; Register BasicObject methods
(register-method "BasicObject" :to_s #(to-s %))
(register-method "BasicObject" :inspect #(inspect %))
(register-method "BasicObject" :== #(ruby-eq %1 %2))
(register-method "BasicObject" :!= #(not (ruby-eq %1 %2)))
(register-method "BasicObject" :equal? #(identical? %1 %2))
(register-method "BasicObject" :object_id #(System/identityHashCode %))

;; =============================================================================
;; Object - Main Ruby Object class (includes Kernel)
;; =============================================================================

(defrecord RubyObjectClass [value]
  RubyObject
  (ruby-class [_] "Object")
  (ruby-ancestors [_] ["Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :class :nil? :== :!= :puts :p :print
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a?} method-name))
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

;; Register Object methods (inherits from BasicObject)
(register-method "Object" :to_s #(to-s %))
(register-method "Object" :inspect #(inspect %))
(register-method "Object" :== #(ruby-eq %1 %2))
(register-method "Object" :!= #(not (ruby-eq %1 %2)))
(register-method "Object" :equal? #(identical? %1 %2))
(register-method "Object" :object_id #(System/identityHashCode %))
(register-method "Object" :class #(ruby-class %))
(register-method "Object" :nil? #(nil? (:value %)))
(register-method "Object" :respond_to? #(respond-to? %1 %2))
(register-method "Object" :methods #(keys (get @method-registry (ruby-class %) {})))
(register-method "Object" :instance_of? #(= (ruby-class %1) %2))
(register-method "Object" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
(register-method "Object" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

;; Kernel methods (mixed into Object)
(register-method "Object" :puts
  (fn [_ & args]
    (if (empty? args)
      (println)
      (doseq [arg args]
        (println (if (satisfies? RubyInspectable arg) (to-s arg) (str arg)))))
    nil))

(register-method "Object" :p
  (fn [_ & args]
    (if (empty? args)
      nil
      (let [results (mapv #(if (satisfies? RubyInspectable %) (inspect %) (pr-str %)) args)]
        (println (str/join " " results))
        (if (= 1 (count results)) (first args) (vec args))))))

(register-method "Object" :print
  (fn [_ & args]
    (doseq [arg args]
      (print (if (satisfies? RubyInspectable arg) (to-s arg) (str arg))))
    nil))

;; =============================================================================
;; String - Ruby String class
;; =============================================================================

(defrecord RubyString [value]
  RubyObject
  (ruby-class [_] "String")
  (ruby-ancestors [_] ["String" "Object" "Kernel" "BasicObject"])
  (respond-to? [_ method-name]
    (contains? #{:to_s :inspect :length :size :upcase :downcase :+ :== :!= 
                 :< :> :<= :>= :<=> :empty? :reverse :capitalize :strip
                 :start_with? :end_with? :include? :index :rindex
                 "[]" :slice :gsub :sub :split :chomp :chop
                 :equal? :object_id :respond_to? :methods :instance_of?
                 :kind_of? :is_a? :class :nil? :puts :p :print} method-name))
  (get-ruby-method [this method-name]
    (method-lookup this method-name))
  
  RubyInspectable
  (to-s [this] (:value this))
  (inspect [this] (str "\"" (:value this) "\""))
  
  RubyComparable
  (ruby-eq [this other]
    (cond
      (instance? RubyString other) (= (:value this) (:value other))
      (string? other) (= (:value this) other)
      :else false))
  (ruby-compare [this other]
    (cond
      (instance? RubyString other) (compare (:value this) (:value other))
      (string? other) (compare (:value this) other)
      :else nil)))

;; Register String methods
(register-method "String" :to_s #(to-s %))
(register-method "String" :inspect #(inspect %))
(register-method "String" :== #(ruby-eq %1 %2))
(register-method "String" :!= #(not (ruby-eq %1 %2)))
(register-method "String" :equal? #(identical? %1 %2))
(register-method "String" :object_id #(System/identityHashCode %))
(register-method "String" :class #(ruby-class %))
(register-method "String" :nil? #(false)) ; Strings are never nil
(register-method "String" :respond_to? #(respond-to? %1 %2))
(register-method "String" :methods #(keys (get @method-registry (ruby-class %) {})))
(register-method "String" :instance_of? #(= (ruby-class %1) %2))
(register-method "String" :kind_of? #(contains? (set (ruby-ancestors %1)) %2))
(register-method "String" :is_a? #(contains? (set (ruby-ancestors %1)) %2))

;; String-specific methods
(register-method "String" :length #(count (:value %)))
(register-method "String" :size #(count (:value %))) ; alias for length

(register-method "String" :upcase 
  #(->RubyString (str/upper-case (:value %))))

(register-method "String" :downcase 
  #(->RubyString (str/lower-case (:value %))))

(register-method "String" :capitalize
  #(->RubyString (str/capitalize (:value %))))

(register-method "String" :reverse
  #(->RubyString (str/reverse (:value %))))

(register-method "String" :strip
  #(->RubyString (str/trim (:value %))))

(register-method "String" :empty?
  #(empty? (:value %)))

;; String concatenation
(register-method "String" :+
  (fn [str1 str2]
    (cond
      (instance? RubyString str2) (->RubyString (str (:value str1) (:value str2)))
      (string? str2) (->RubyString (str (:value str1) str2))
      :else (throw (ex-info "String concatenation requires string argument" 
                           {:str1 str1 :str2 str2})))))

;; String comparison operators
(register-method "String" :<
  (fn [str1 str2]
    (let [val1 (:value str1)
          val2 (if (instance? RubyString str2) (:value str2) str2)]
      (< (compare val1 val2) 0))))

(register-method "String" :>
  (fn [str1 str2]
    (let [val1 (:value str1)
          val2 (if (instance? RubyString str2) (:value str2) str2)]
      (> (compare val1 val2) 0))))

(register-method "String" :<=
  (fn [str1 str2]
    (let [val1 (:value str1)
          val2 (if (instance? RubyString str2) (:value str2) str2)]
      (<= (compare val1 val2) 0))))

(register-method "String" :>=
  (fn [str1 str2]
    (let [val1 (:value str1)
          val2 (if (instance? RubyString str2) (:value str2) str2)]
      (>= (compare val1 val2) 0))))

(register-method "String" :<=>
  (fn [str1 str2]
    (let [val1 (:value str1)
          val2 (if (instance? RubyString str2) (:value str2) str2)]
      (compare val1 val2))))

;; String query methods
(register-method "String" :start_with?
  (fn [str prefix]
    (let [str-val (:value str)
          prefix-val (if (instance? RubyString prefix) (:value prefix) prefix)]
      (str/starts-with? str-val prefix-val))))

(register-method "String" :end_with?
  (fn [str suffix]
    (let [str-val (:value str)
          suffix-val (if (instance? RubyString suffix) (:value suffix) suffix)]
      (str/ends-with? str-val suffix-val))))

(register-method "String" :include?
  (fn [str substring]
    (let [str-val (:value str)
          sub-val (if (instance? RubyString substring) (:value substring) substring)]
      (str/includes? str-val sub-val))))

;; String indexing
(register-method "String" :index
  (fn [str substring]
    (let [str-val (:value str)
          sub-val (if (instance? RubyString substring) (:value substring) substring)
          idx (str/index-of str-val sub-val)]
      (if (= idx -1) nil idx))))

(register-method "String" :rindex
  (fn [str substring]
    (let [str-val (:value str)
          sub-val (if (instance? RubyString substring) (:value substring) substring)
          idx (str/last-index-of str-val sub-val)]
      (if (= idx -1) nil idx))))

;; String slicing (basic implementation)
(register-method "String" "[]"
  (fn [str index & args]
    (let [str-val (:value str)
          len (count str-val)]
      (cond
        (and (integer? index) (empty? args))
        ;; Single character access: str[index]
        (if (and (>= index 0) (< index len))
          (->RubyString (str (nth str-val index)))
          nil)
        
        (and (integer? index) (= 1 (count args)) (integer? (first args)))
        ;; Substring: str[start, length]
        (let [start index
              length (first args)]
          (if (and (>= start 0) (< start len) (> length 0))
            (let [end-pos (min (+ start length) len)]
              (->RubyString (subs str-val start end-pos)))
            (->RubyString "")))
        
        :else
        (throw (ex-info "Invalid string indexing arguments" 
                       {:index index :args args}))))))

(register-method "String" :slice 
  (fn [string-obj index & args]
    (let [str-val (:value string-obj)
          len (count str-val)]
      (cond
        (and (integer? index) (empty? args))
        ;; Single character access: str.slice(index)
        (if (and (>= index 0) (< index len))
          (->RubyString (str (nth str-val index)))
          nil)
        
        (and (integer? index) (= 1 (count args)) (integer? (first args)))
        ;; Substring: str.slice(start, length)
        (let [start index
              length (first args)]
          (if (and (>= start 0) (< start len) (> length 0))
            (let [end-pos (min (+ start length) len)]
              (->RubyString (subs str-val start end-pos)))
            (->RubyString "")))
        
        :else
        (throw (ex-info "Invalid string slicing arguments" 
                       {:index index :args args}))))))

;; String manipulation
(register-method "String" :chomp
  (fn [string-obj & args]
    (let [str-val (:value string-obj)
          separator (if (empty? args) "\n" (first args))
          sep-val (if (instance? RubyString separator) (:value separator) separator)]
      (->RubyString 
        (if (str/ends-with? str-val sep-val)
          (subs str-val 0 (- (count str-val) (count sep-val)))
          str-val)))))

(register-method "String" :chop
  (fn [string-obj]
    (let [str-val (:value string-obj)]
      (if (empty? str-val)
        (->RubyString "")
        (->RubyString (subs str-val 0 (dec (count str-val))))))))

;; String splitting (basic implementation)
(register-method "String" :split
  (fn [string-obj & args]
    (let [str-val (:value string-obj)
          delimiter (if (empty? args) #"\s+" (first args))
          delim-val (if (instance? RubyString delimiter) (:value delimiter) delimiter)]
      (mapv ->RubyString (str/split str-val (re-pattern delim-val))))))

;; Inherit Kernel methods
(register-method "String" :puts 
  (fn [this & args]
    (if (empty? args)
      (println)
      (doseq [arg args]
        (println (if (satisfies? RubyInspectable arg) (to-s arg) (str arg)))))
    nil))

(register-method "String" :p 
  (fn [this & args]
    (if (empty? args)
      nil
      (let [results (mapv #(if (satisfies? RubyInspectable %) (inspect %) (pr-str %)) args)]
        (println (str/join " " results))
        (if (= 1 (count results)) (first args) (vec args))))))

(register-method "String" :print 
  (fn [this & args]
    (doseq [arg args]
      (print (if (satisfies? RubyInspectable arg) (to-s arg) (str arg))))
    nil))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn create-basic-object
  "Create a new BasicObject instance."
  []
  (->BasicObject))

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

(defn create-string
  "Create a new RubyString instance."
  [value]
  (->RubyString (str value)))

;; =============================================================================
;; Method Call Interface
;; =============================================================================

(defn invoke-ruby-method
  "Main interface for calling Ruby methods on objects."
  [obj method-name & args]
  (cond
    (satisfies? RubyObject obj)
    (apply call-ruby-method obj method-name args)

    ;; Fallback for non-Ruby objects (backward compatibility)
    :else
    (throw (ex-info (str "Cannot call method " method-name " on non-Ruby object")
                    {:object obj :method method-name :args args}))))

;; =============================================================================
;; Debugging and Introspection
;; =============================================================================

(defn debug-method-registry
  "Return the current method registry for debugging."
  []
  @method-registry)

(defn class-methods
  "Get all methods for a Ruby class."
  [class-name]
  (keys (get @method-registry class-name {})))

(defn all-ruby-classes
  "Get all registered Ruby classes."
  []
  (keys @method-registry))

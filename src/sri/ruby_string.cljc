(ns sri.ruby-string
  "Ruby String class implementation."
  (:require [clojure.string :as str]
            [sri.ruby-protocols :refer [RubyObject RubyInspectable RubyComparable
                                        ruby-class ruby-ancestors respond-to?
                                        to-s inspect ruby-eq ruby-compare]]
            [sri.ruby-method-registry :refer [register-method method-lookup class-methods]]
            [sri.ruby-kernel :as kernel]))

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

;; =============================================================================
;; Method Registration
;; =============================================================================

(defn register-string-methods!
  "Register all String methods in the method registry."
  []
  ;; Inherit from Object
  (register-method "String" :to_s #(to-s %))
  (register-method "String" :inspect #(inspect %))
  (register-method "String" :== #(ruby-eq %1 %2))
  (register-method "String" :!= #(not (ruby-eq %1 %2)))
  (register-method "String" :equal? #(identical? %1 %2))
  (register-method "String" :object_id #(System/identityHashCode %))
  (register-method "String" :class #(ruby-class %))
  (register-method "String" :nil? #(false)) ; Strings are never nil
  (register-method "String" :respond_to? #(respond-to? %1 %2))
  (register-method "String" :methods #(class-methods (ruby-class %)))
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
        ;; Ruby String + Ruby String
        (instance? RubyString str2) (->RubyString (str (:value str1) (:value str2)))
        ;; Ruby String + Java String
        (string? str2) (->RubyString (str (:value str1) str2))
        ;; Ruby String + Number or other types (convert to string)
        :else (->RubyString (str (:value str1) (str str2))))))

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

  ;; Include Kernel methods (mixed into Object, inherited by String)
  (kernel/register-kernel-methods-for-class! "String"))

;; Register methods on namespace load
(register-string-methods!)

;; =============================================================================
;; Constructor Function
;; =============================================================================

(defn create-string
  "Create a new RubyString instance."
  [value]
  (cond
    ;; If already a RubyString, extract its value
    (instance? RubyString value) (->RubyString (:value value))
    ;; If it's a string or other value, convert to string
    :else (->RubyString (str value))))
